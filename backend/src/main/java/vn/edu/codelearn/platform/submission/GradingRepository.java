package vn.edu.codelearn.platform.submission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class GradingRepository {

    private final JdbcClient jdbc;

    GradingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Long> nextQueuedSubmissionId() {
        return jdbc.sql(
                        """
                SELECT id
                FROM submission
                WHERE status = 'QUEUED'
                  AND (retry_after IS NULL OR retry_after <= CURRENT_TIMESTAMP)
                ORDER BY created_at, id
                LIMIT 1
                """)
                .query(Long.class)
                .optional();
    }

    @Transactional
    Optional<SubmissionViews.WorkerSubmission> claim(long submissionId) {
        int updated =
                jdbc.sql(
                                """
                        UPDATE submission
                        SET status = 'RUNNING', attempt_count = attempt_count + 1,
                            lease_expires_at = NULL,
                            status_updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND status = 'QUEUED'
                          AND (retry_after IS NULL OR retry_after <= CURRENT_TIMESTAMP)
                        """)
                        .param("id", submissionId)
                        .update();
        return updated == 1 ? findWorkerSubmission(submissionId) : Optional.empty();
    }

    boolean extendLease(SubmissionViews.WorkerSubmission submission, long seconds) {
        return jdbc.sql(
                                """
                UPDATE submission
                SET lease_expires_at = CURRENT_TIMESTAMP + make_interval(secs => :seconds)
                WHERE id = :id AND status = 'RUNNING' AND attempt_count = :attempt
                """)
                        .param("id", submission.id())
                        .param("attempt", submission.attempt())
                        .param("seconds", seconds)
                        .update()
                == 1;
    }

    List<SubmissionViews.WorkerTestCase> findTestCases(long exerciseId) {
        return jdbc.sql(
                        """
                SELECT id, position, stdin, expected_output, weight, is_public
                FROM test_case
                WHERE exercise_id = :exerciseId
                ORDER BY position
                """)
                .param("exerciseId", exerciseId)
                .query(
                        (row, index) ->
                                new SubmissionViews.WorkerTestCase(
                                        row.getLong("id"),
                                        row.getInt("position"),
                                        row.getString("stdin"),
                                        row.getString("expected_output"),
                                        row.getInt("weight"),
                                        row.getBoolean("is_public")))
                .list();
    }

    @Transactional
    boolean complete(
            SubmissionViews.WorkerSubmission submission, SubmissionViews.GradeResult grade) {
        int updated =
                jdbc.sql(
                                """
                        UPDATE submission
                        SET status = 'DONE', verdict = :verdict, score = :score,
                            time_ms = :timeMs, memory_kb = :memoryKb,
                            error_message = :errorMessage, finished_at = CURRENT_TIMESTAMP,
                            status_updated_at = CURRENT_TIMESTAMP, retry_after = NULL,
                            lease_expires_at = NULL
                        WHERE id = :id AND status = 'RUNNING' AND attempt_count = :attempt
                        """)
                        .param("verdict", grade.verdict())
                        .param("score", grade.score())
                        .param("timeMs", grade.timeMs())
                        .param("memoryKb", grade.memoryKb())
                        .param("errorMessage", grade.errorMessage())
                        .param("id", submission.id())
                        .param("attempt", submission.attempt())
                        .update();
        if (updated != 1) {
            return false;
        }
        insertResults(submission.id(), grade.tests());
        return true;
    }

    @Transactional
    boolean retryOrFail(
            SubmissionViews.WorkerSubmission submission, long maxAttempts, long delaySeconds) {
        String status = submission.attempt() < maxAttempts ? "QUEUED" : "FAILED";
        int updated =
                jdbc.sql(
                                """
                        UPDATE submission
                        SET status = :status, status_updated_at = CURRENT_TIMESTAMP,
                            lease_expires_at = NULL,
                            retry_after = CASE WHEN :status = 'QUEUED'
                                THEN CURRENT_TIMESTAMP + make_interval(secs => :delay)
                                ELSE NULL END,
                            finished_at = CASE WHEN :status = 'FAILED'
                                THEN CURRENT_TIMESTAMP ELSE NULL END,
                            error_message = CASE WHEN :status = 'FAILED'
                                THEN 'Grader is unavailable. Please try again.' ELSE NULL END,
                            verdict = NULL, score = NULL, time_ms = NULL, memory_kb = NULL
                        WHERE id = :id AND status = 'RUNNING' AND attempt_count = :attempt
                        """)
                        .param("status", status)
                        .param("delay", delaySeconds)
                        .param("id", submission.id())
                        .param("attempt", submission.attempt())
                        .update();
        return updated == 1 && status.equals("QUEUED");
    }

    List<SubmissionViews.StuckSubmission> findStuck(long thresholdSeconds, int batchSize) {
        return jdbc.sql(
                        """
                SELECT id, status, attempt_count, status_updated_at
                FROM submission
                WHERE status IN ('QUEUED', 'RUNNING')
                  AND COALESCE(lease_expires_at,
                      status_updated_at + make_interval(secs => :threshold)) < CURRENT_TIMESTAMP
                  AND (status = 'RUNNING' OR retry_after IS NULL OR retry_after <= CURRENT_TIMESTAMP)
                ORDER BY status_updated_at
                LIMIT :batchSize
                """)
                .param("threshold", thresholdSeconds)
                .param("batchSize", batchSize)
                .query(
                        (row, index) ->
                                new SubmissionViews.StuckSubmission(
                                        row.getLong("id"),
                                        row.getString("status"),
                                        row.getInt("attempt_count"),
                                        row.getObject("status_updated_at", OffsetDateTime.class)))
                .list();
    }

    @Transactional
    boolean recoverStuck(
            SubmissionViews.StuckSubmission stuck,
            long thresholdSeconds,
            long maxAttempts,
            long retryDelaySeconds) {
        String status = stuck.attempt() >= maxAttempts ? "FAILED" : "QUEUED";
        int updated =
                jdbc.sql(
                                """
                        UPDATE submission
                        SET status = :nextStatus, status_updated_at = CURRENT_TIMESTAMP,
                            lease_expires_at = NULL,
                            retry_after = CASE WHEN :nextStatus = 'QUEUED'
                                THEN CURRENT_TIMESTAMP + make_interval(secs => :delay)
                                ELSE NULL END,
                            finished_at = CASE WHEN :nextStatus = 'FAILED'
                                THEN CURRENT_TIMESTAMP ELSE NULL END,
                            error_message = CASE WHEN :nextStatus = 'FAILED'
                                THEN 'Grader is unavailable. Please try again.' ELSE NULL END,
                            verdict = NULL, score = NULL, time_ms = NULL, memory_kb = NULL
                        WHERE id = :id AND status = :currentStatus
                          AND status_updated_at = :statusUpdatedAt
                          AND attempt_count = :attempt
                          AND COALESCE(lease_expires_at,
                              status_updated_at + make_interval(secs => :threshold)) < CURRENT_TIMESTAMP
                          AND (status = 'RUNNING' OR retry_after IS NULL OR retry_after <= CURRENT_TIMESTAMP)
                        """)
                        .param("nextStatus", status)
                        .param("delay", retryDelaySeconds)
                        .param("id", stuck.id())
                        .param("currentStatus", stuck.status())
                        .param("statusUpdatedAt", stuck.statusUpdatedAt())
                        .param("attempt", stuck.attempt())
                        .param("threshold", thresholdSeconds)
                        .update();
        return updated == 1 && status.equals("QUEUED");
    }

    private Optional<SubmissionViews.WorkerSubmission> findWorkerSubmission(long id) {
        return jdbc.sql(
                        """
                SELECT s.id, s.exercise_id, s.language, s.source_code,
                       e.max_score, s.attempt_count
                FROM submission s
                JOIN exercise e ON e.id = s.exercise_id
                WHERE s.id = :id AND s.status = 'RUNNING'
                """)
                .param("id", id)
                .query(
                        (row, index) ->
                                new SubmissionViews.WorkerSubmission(
                                        row.getLong("id"),
                                        row.getLong("exercise_id"),
                                        row.getString("language"),
                                        row.getString("source_code"),
                                        row.getInt("max_score"),
                                        row.getInt("attempt_count")))
                .optional();
    }

    private void insertResults(long submissionId, List<SubmissionViews.WorkerTestResult> results) {
        for (var result : results) {
            jdbc.sql(
                            """
                    INSERT INTO submission_test_result (
                        submission_id, test_case_id, passed, verdict, time_ms, memory_kb, actual_output
                    )
                    VALUES (
                        :submissionId, :testCaseId, :passed, :verdict, :timeMs, :memoryKb, :actualOutput
                    )
                    """)
                    .param("submissionId", submissionId)
                    .param("testCaseId", result.testCaseId())
                    .param("passed", result.passed())
                    .param("verdict", result.verdict())
                    .param("timeMs", result.timeMs())
                    .param("memoryKb", result.memoryKb())
                    .param("actualOutput", result.isPublic() ? result.actualOutput() : null)
                    .update();
        }
    }
}

package vn.edu.codelearn.platform.submission;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class SubmissionRepository {

    private final JdbcClient jdbc;

    SubmissionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    boolean publishedExerciseExists(long exerciseId) {
        return jdbc.sql(
                                """
                SELECT count(*)
                FROM exercise
                JOIN published_lesson lesson ON lesson.id = exercise.lesson_id
                WHERE exercise.id = :exerciseId
                """)
                        .param("exerciseId", exerciseId)
                        .query(Long.class)
                        .single()
                > 0;
    }

    boolean hasHistory(long userId, long exerciseId) {
        return jdbc.sql(
                        """
                SELECT EXISTS (
                    SELECT 1 FROM submission WHERE user_id = :userId AND exercise_id = :exerciseId
                )
                """)
                .param("userId", userId)
                .param("exerciseId", exerciseId)
                .query(Boolean.class)
                .single();
    }

    int countPendingForUser(long userId) {
        return jdbc.sql(
                        """
                SELECT count(*)
                FROM submission
                WHERE user_id = :userId AND status IN ('QUEUED', 'RUNNING')
                """)
                .param("userId", userId)
                .query(Integer.class)
                .single();
    }

    void lockAccountForSubmissions(long userId) {
        jdbc.sql("SELECT id FROM account WHERE id = :userId FOR UPDATE")
                .param("userId", userId)
                .query(Long.class)
                .single();
    }

    SubmissionViews.Receipt insert(
            long userId, long exerciseId, String language, String sourceCode) {
        return jdbc.sql(
                        """
                INSERT INTO submission (user_id, exercise_id, language, source_code, status)
                VALUES (:userId, :exerciseId, :language, :sourceCode, 'QUEUED')
                RETURNING id, status, verdict, created_at
                """)
                .param("userId", userId)
                .param("exerciseId", exerciseId)
                .param("language", language)
                .param("sourceCode", sourceCode)
                .query(
                        (row, index) ->
                                new SubmissionViews.Receipt(
                                        row.getLong("id"),
                                        row.getString("status"),
                                        row.getString("verdict"),
                                        row.getObject(
                                                "created_at", java.time.OffsetDateTime.class)))
                .single();
    }

    void failBeforeQueueing(long submissionId) {
        jdbc.sql(
                        """
                UPDATE submission
                SET status = 'FAILED', status_updated_at = CURRENT_TIMESTAMP,
                    finished_at = CURRENT_TIMESTAMP, error_message = 'Grader is unavailable.'
                WHERE id = :id AND status = 'QUEUED'
                """)
                .param("id", submissionId)
                .update();
    }

    Optional<SubmissionViews.SubmissionRow> findForUser(long submissionId, long userId) {
        return jdbc.sql(
                        """
                SELECT s.id, s.exercise_id, s.status, s.verdict, s.score, e.max_score,
                       s.time_ms, s.memory_kb, s.error_message, s.created_at, s.finished_at
                FROM submission s
                JOIN exercise e ON e.id = s.exercise_id
                WHERE s.id = :submissionId AND s.user_id = :userId
                """)
                .param("submissionId", submissionId)
                .param("userId", userId)
                .query(SUBMISSION_ROW)
                .optional();
    }

    Optional<SubmissionViews.DetailRow> findDetailForUser(long submissionId, long userId) {
        return jdbc.sql(
                        """
                SELECT s.id, s.exercise_id, s.status, s.verdict, s.score, e.max_score,
                       s.time_ms, s.memory_kb, s.error_message, s.created_at, s.finished_at,
                       s.language, s.source_code
                FROM submission s
                JOIN exercise e ON e.id = s.exercise_id
                WHERE s.id = :submissionId AND s.user_id = :userId
                """)
                .param("submissionId", submissionId)
                .param("userId", userId)
                .query(
                        (row, index) ->
                                new SubmissionViews.DetailRow(
                                        row.getLong("id"),
                                        row.getLong("exercise_id"),
                                        row.getString("status"),
                                        row.getString("verdict"),
                                        row.getBigDecimal("score"),
                                        row.getInt("max_score"),
                                        row.getObject("time_ms", Integer.class),
                                        row.getObject("memory_kb", Integer.class),
                                        row.getString("error_message"),
                                        row.getObject("created_at", java.time.OffsetDateTime.class),
                                        row.getObject(
                                                "finished_at", java.time.OffsetDateTime.class),
                                        row.getString("language"),
                                        row.getString("source_code")))
                .optional();
    }

    SubmissionViews.Detail toDetail(SubmissionViews.DetailRow row) {
        return new SubmissionViews.Detail(
                row.submissionId(),
                row.exerciseId(),
                row.status(),
                row.verdict(),
                row.score(),
                row.maxScore(),
                row.timeMs(),
                row.memoryKb(),
                row.errorMessage(),
                row.createdAt(),
                row.finishedAt(),
                findTestResults(row.submissionId()),
                row.language(),
                row.sourceCode());
    }

    List<SubmissionViews.TestResult> findTestResults(long submissionId) {
        return jdbc.sql(
                        """
                SELECT tc.position, tc.is_public, tr.passed, tr.verdict, tr.time_ms, tr.memory_kb,
                       public_case.stdin, public_case.expected_output,
                       CASE WHEN tc.is_public THEN tr.actual_output END AS actual_output
                FROM submission_test_result tr
                JOIN test_case tc ON tc.id = tr.test_case_id
                LEFT JOIN test_case_public public_case ON public_case.id = tc.id
                WHERE tr.submission_id = :submissionId
                ORDER BY tc.position
                """)
                .param("submissionId", submissionId)
                .query(
                        (row, index) ->
                                new SubmissionViews.TestResult(
                                        row.getInt("position"),
                                        row.getBoolean("is_public"),
                                        row.getBoolean("passed"),
                                        row.getString("verdict"),
                                        row.getObject("time_ms", Integer.class),
                                        row.getObject("memory_kb", Integer.class),
                                        row.getString("stdin"),
                                        row.getString("expected_output"),
                                        row.getString("actual_output")))
                .list();
    }

    List<SubmissionViews.Summary> findHistory(long userId, long exerciseId, int page, int size) {
        return jdbc.sql(
                        """
                SELECT id, status, verdict, score, time_ms, memory_kb, created_at
                FROM submission
                WHERE user_id = :userId AND exercise_id = :exerciseId
                ORDER BY created_at DESC, id DESC
                LIMIT :size OFFSET :offset
                """)
                .param("userId", userId)
                .param("exerciseId", exerciseId)
                .param("size", size)
                .param("offset", (long) page * size)
                .query(
                        (row, index) ->
                                new SubmissionViews.Summary(
                                        row.getLong("id"),
                                        row.getString("status"),
                                        row.getString("verdict"),
                                        row.getBigDecimal("score"),
                                        row.getObject("time_ms", Integer.class),
                                        row.getObject("memory_kb", Integer.class),
                                        row.getObject(
                                                "created_at", java.time.OffsetDateTime.class)))
                .list();
    }

    SubmissionViews.View toView(SubmissionViews.SubmissionRow row) {
        return new SubmissionViews.View(
                row.submissionId(),
                row.exerciseId(),
                row.status(),
                row.verdict(),
                row.score(),
                row.maxScore(),
                row.timeMs(),
                row.memoryKb(),
                row.errorMessage(),
                row.createdAt(),
                row.finishedAt(),
                findTestResults(row.submissionId()));
    }

    private static final org.springframework.jdbc.core.RowMapper<SubmissionViews.SubmissionRow>
            SUBMISSION_ROW =
                    (row, index) ->
                            new SubmissionViews.SubmissionRow(
                                    row.getLong("id"),
                                    row.getLong("exercise_id"),
                                    row.getString("status"),
                                    row.getString("verdict"),
                                    row.getBigDecimal("score"),
                                    row.getInt("max_score"),
                                    row.getObject("time_ms", Integer.class),
                                    row.getObject("memory_kb", Integer.class),
                                    row.getString("error_message"),
                                    row.getObject("created_at", java.time.OffsetDateTime.class),
                                    row.getObject("finished_at", java.time.OffsetDateTime.class));
}

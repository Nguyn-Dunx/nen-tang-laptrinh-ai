package vn.edu.codelearn.platform.submission;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

final class SubmissionViews {

    private SubmissionViews() {}

    record CreateSubmission(
            @NotBlank String language, @NotBlank @JsonProperty("source_code") String sourceCode) {}

    record Receipt(
            @JsonProperty("submission_id") long submissionId,
            String status,
            String verdict,
            @JsonProperty("created_at") OffsetDateTime createdAt) {}

    record TestResult(
            int position,
            @JsonProperty("public") boolean isPublic,
            boolean passed,
            String verdict,
            @JsonProperty("time_ms") Integer timeMs,
            @JsonProperty("memory_kb") Integer memoryKb,
            String stdin,
            @JsonProperty("expected_output") String expectedOutput,
            @JsonProperty("actual_output") String actualOutput) {}

    record View(
            @JsonProperty("submission_id") long submissionId,
            @JsonProperty("exercise_id") long exerciseId,
            String status,
            String verdict,
            BigDecimal score,
            @JsonProperty("max_score") int maxScore,
            @JsonProperty("time_ms") Integer timeMs,
            @JsonProperty("memory_kb") Integer memoryKb,
            @JsonProperty("error_message") String errorMessage,
            @JsonProperty("created_at") OffsetDateTime createdAt,
            @JsonProperty("finished_at") OffsetDateTime finishedAt,
            List<TestResult> tests) {}

    record Detail(
            @JsonProperty("submission_id") long submissionId,
            @JsonProperty("exercise_id") long exerciseId,
            String status,
            String verdict,
            BigDecimal score,
            @JsonProperty("max_score") int maxScore,
            @JsonProperty("time_ms") Integer timeMs,
            @JsonProperty("memory_kb") Integer memoryKb,
            @JsonProperty("error_message") String errorMessage,
            @JsonProperty("created_at") OffsetDateTime createdAt,
            @JsonProperty("finished_at") OffsetDateTime finishedAt,
            List<TestResult> tests,
            String language,
            @JsonProperty("source_code") String sourceCode) {}

    record Summary(
            @JsonProperty("submission_id") long submissionId,
            String status,
            String verdict,
            BigDecimal score,
            @JsonProperty("time_ms") Integer timeMs,
            @JsonProperty("memory_kb") Integer memoryKb,
            @JsonProperty("created_at") OffsetDateTime createdAt) {}

    record Page(List<Summary> items, int page, int size) {}

    record SubmissionRow(
            long submissionId,
            long exerciseId,
            String status,
            String verdict,
            BigDecimal score,
            int maxScore,
            Integer timeMs,
            Integer memoryKb,
            String errorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime finishedAt) {}

    record WorkerSubmission(
            long id,
            long exerciseId,
            String language,
            String sourceCode,
            int maxScore,
            int attempt) {}

    record WorkerTestCase(
            long id,
            int position,
            String stdin,
            String expectedOutput,
            int weight,
            boolean isPublic) {}

    record WorkerTestResult(
            long testCaseId,
            int position,
            boolean isPublic,
            boolean passed,
            String verdict,
            Integer timeMs,
            Integer memoryKb,
            String actualOutput) {}

    record GradeResult(
            String verdict,
            BigDecimal score,
            Integer timeMs,
            Integer memoryKb,
            String errorMessage,
            List<WorkerTestResult> tests) {}

    record DetailRow(
            long submissionId,
            long exerciseId,
            String status,
            String verdict,
            BigDecimal score,
            int maxScore,
            Integer timeMs,
            Integer memoryKb,
            String errorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime finishedAt,
            String language,
            String sourceCode) {}

    record StuckSubmission(long id, String status, int attempt, OffsetDateTime statusUpdatedAt) {}
}

package vn.edu.codelearn.platform.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.codelearn.platform.sandbox.SandboxExecutionException;
import vn.edu.codelearn.platform.sandbox.SandboxResult;

class GradingRulesTest {

    @Test
    void normalizesLineEndingsAndTrailingWhitespaceOnly() {
        assertThat(GradingRules.normalize("5 \t\r\n")).isEqualTo("5");
        assertThat(GradingRules.normalize(" 5\n")).isEqualTo(" 5");
        assertThat(GradingRules.normalize("a  b\n")).isEqualTo("a  b");
        assertThat(GradingRules.normalize("a \r\n \t\n b\t\r")).isEqualTo("a\n\n b");
    }

    @Test
    void mapsJudgeStatusesWithoutSendingExpectedOutputToJudge0() {
        var test = testCase(1, 1, true, "answer");
        var accepted =
                GradingRules.evaluate(test, result(3, "answer\n", null, 0.02, 1024.0, false), 2048);
        var wrongAnswer =
                GradingRules.evaluate(
                        test, result(3, "incorrect", null, 0.03, 1024.0, false), 2048);
        var timeLimit = GradingRules.evaluate(test, result(5, "", null, 2.0, 1024.0, false), 2048);
        var memoryLimit =
                GradingRules.evaluate(
                        test, result(7, "", "RuntimeError", 0.03, 2048.0, false), 2048);

        assertThat(accepted.verdict()).isEqualTo("ACCEPTED");
        assertThat(wrongAnswer.verdict()).isEqualTo("WRONG_ANSWER");
        assertThat(timeLimit.verdict()).isEqualTo("TIME_LIMIT");
        assertThat(memoryLimit.verdict()).isEqualTo("MEMORY_LIMIT");
    }

    @Test
    void neverReturnsOutputForHiddenTestsAndRejectsJudge0WrongAnswerStatus() {
        var hiddenTest = testCase(2, 1, false, "hidden answer");
        var result =
                GradingRules.evaluate(
                        hiddenTest, result(3, "hidden answer", null, 0.01, 512.0, false), 2048);

        assertThat(result.actualOutput()).isNull();
        assertThatThrownBy(
                        () ->
                                GradingRules.evaluate(
                                        hiddenTest, result(4, "", null, 0.01, 512.0, false), 2048))
                .isInstanceOf(SandboxExecutionException.class);
    }

    @Test
    void aggregatesWeightedScoresAndPrioritizesRuntimeErrors() {
        var tests = List.of(testCase(1, 1, true, "x"), testCase(2, 3, false, "y"));
        var results =
                List.of(
                        workerResult(1, 1, true, true, "ACCEPTED"),
                        workerResult(2, 2, false, false, "TIME_LIMIT"));
        var grade = GradingRules.aggregate(results, tests, 100);

        assertThat(grade.verdict()).isEqualTo("TIME_LIMIT");
        assertThat(grade.score()).isEqualByComparingTo("25.00");
    }

    @Test
    void compileErrorStopsScoringAndHasNoRuntimeMetrics() {
        var tests = List.of(testCase(1, 1, true, "x"), testCase(2, 1, false, "y"));
        var grade =
                GradingRules.aggregate(
                        List.of(workerResult(1, 1, true, false, "COMPILE_ERROR")), tests, 100);

        assertThat(grade.verdict()).isEqualTo("COMPILE_ERROR");
        assertThat(grade.score()).isEqualByComparingTo("0.00");
        assertThat(grade.timeMs()).isNull();
        assertThat(grade.memoryKb()).isNull();
    }

    private SubmissionViews.WorkerTestCase testCase(
            long id, int weight, boolean isPublic, String expectedOutput) {
        return new SubmissionViews.WorkerTestCase(
                id, (int) id, "", expectedOutput, weight, isPublic);
    }

    private SubmissionViews.WorkerTestResult workerResult(
            long id, int position, boolean isPublic, boolean passed, String verdict) {
        return new SubmissionViews.WorkerTestResult(
                id, position, isPublic, passed, verdict, 10, 512, null);
    }

    private SandboxResult result(
            int status,
            String stdout,
            String stderr,
            Double time,
            Double memory,
            boolean truncated) {
        return new SandboxResult(status, stdout, stderr, time, memory, truncated);
    }
}

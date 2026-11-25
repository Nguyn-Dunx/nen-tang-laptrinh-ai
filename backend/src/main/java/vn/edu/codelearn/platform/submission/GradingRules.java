package vn.edu.codelearn.platform.submission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import vn.edu.codelearn.platform.sandbox.SandboxExecutionException;
import vn.edu.codelearn.platform.sandbox.SandboxResult;

final class GradingRules {

    private GradingRules() {}

    static SubmissionViews.WorkerTestResult evaluate(
            SubmissionViews.WorkerTestCase test, SandboxResult run, long memoryLimitKb) {
        String verdict =
                switch (run.statusId()) {
                    case 3 -> acceptedVerdict(test, run, memoryLimitKb);
                    case 5 -> "TIME_LIMIT";
                    case 6 -> "COMPILE_ERROR";
                    case 7, 8, 9, 10, 11, 12 -> runtimeVerdict(run, memoryLimitKb);
                    case 4 ->
                            throw new SandboxExecutionException(
                                    "Judge0 compared output despite no expected output being sent.");
                    default ->
                            throw new SandboxExecutionException(
                                    "Judge0 returned an unsupported status.");
                };
        boolean passed = "ACCEPTED".equals(verdict);
        boolean compileError = "COMPILE_ERROR".equals(verdict);
        return new SubmissionViews.WorkerTestResult(
                test.id(),
                test.position(),
                test.isPublic(),
                passed,
                verdict,
                compileError ? null : milliseconds(run.timeSeconds()),
                compileError ? null : kilobytes(run.memoryKb()),
                test.isPublic() && !compileError ? run.stdout() : null);
    }

    static String normalize(String output) {
        String normalized = output.replace("\r\n", "\n").replace('\r', '\n');
        var trimmed = new StringBuilder(normalized.length());
        int contentEnd = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (character == '\n') {
                trimmed.setLength(contentEnd);
                trimmed.append('\n');
                contentEnd = trimmed.length();
            } else {
                trimmed.append(character);
                if (character != ' ' && character != '\t') {
                    contentEnd = trimmed.length();
                }
            }
        }
        while (contentEnd > 0 && trimmed.charAt(contentEnd - 1) == '\n') {
            contentEnd--;
        }
        return trimmed.substring(0, contentEnd);
    }

    static SubmissionViews.GradeResult aggregate(
            List<SubmissionViews.WorkerTestResult> results,
            List<SubmissionViews.WorkerTestCase> tests,
            int maxScore) {
        var compileError =
                results.stream()
                        .filter(result -> "COMPILE_ERROR".equals(result.verdict()))
                        .findFirst();
        if (compileError.isPresent()) {
            return compileError(results);
        }

        long totalWeight = tests.stream().mapToLong(SubmissionViews.WorkerTestCase::weight).sum();
        if (totalWeight <= 0) {
            throw new SandboxExecutionException("Exercise has no positive test weight.");
        }
        String verdict = verdict(results);
        return new SubmissionViews.GradeResult(
                verdict,
                score(results, tests, maxScore, totalWeight),
                maxMetric(results, SubmissionViews.WorkerTestResult::timeMs),
                maxMetric(results, SubmissionViews.WorkerTestResult::memoryKb),
                verdict.equals("ACCEPTED") ? null : "Your submission did not pass all tests.",
                results);
    }

    private static SubmissionViews.GradeResult compileError(
            List<SubmissionViews.WorkerTestResult> results) {
        return new SubmissionViews.GradeResult(
                "COMPILE_ERROR",
                BigDecimal.ZERO.setScale(2),
                null,
                null,
                "Compilation or syntax error.",
                results);
    }

    private static BigDecimal score(
            List<SubmissionViews.WorkerTestResult> results,
            List<SubmissionViews.WorkerTestCase> tests,
            int maxScore,
            long totalWeight) {
        long passedWeight =
                results.stream()
                        .filter(SubmissionViews.WorkerTestResult::passed)
                        .mapToLong(
                                result ->
                                        tests.stream()
                                                .filter(test -> test.id() == result.testCaseId())
                                                .findFirst()
                                                .orElseThrow()
                                                .weight())
                        .sum();
        return BigDecimal.valueOf(maxScore)
                .multiply(BigDecimal.valueOf(passedWeight))
                .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.DOWN);
    }

    private static String verdict(List<SubmissionViews.WorkerTestResult> results) {
        return results.stream()
                .filter(result -> !result.passed())
                .map(SubmissionViews.WorkerTestResult::verdict)
                .min(Comparator.comparingInt(GradingRules::priority))
                .orElse("ACCEPTED");
    }

    private static Integer maxMetric(
            List<SubmissionViews.WorkerTestResult> results,
            java.util.function.Function<SubmissionViews.WorkerTestResult, Integer> metric) {
        return results.stream()
                .map(metric)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private static String acceptedVerdict(
            SubmissionViews.WorkerTestCase test, SandboxResult run, long memoryLimitKb) {
        if (isOverMemory(run, memoryLimitKb)) {
            return "MEMORY_LIMIT";
        }
        return !run.stdoutTruncated()
                        && normalize(run.stdout()).equals(normalize(test.expectedOutput()))
                ? "ACCEPTED"
                : "WRONG_ANSWER";
    }

    private static String runtimeVerdict(SandboxResult run, long memoryLimitKb) {
        if (isPythonSyntaxError(run.stderr())) {
            return "COMPILE_ERROR";
        }
        return isOverMemory(run, memoryLimitKb) ? "MEMORY_LIMIT" : "RUNTIME_ERROR";
    }

    private static boolean isOverMemory(SandboxResult run, long memoryLimitKb) {
        return run.memoryKb() != null && run.memoryKb() >= memoryLimitKb;
    }

    private static boolean isPythonSyntaxError(String diagnostic) {
        if (diagnostic == null) {
            return false;
        }
        String lastLine =
                diagnostic
                        .lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .reduce((first, second) -> second)
                        .orElse("");
        return lastLine.startsWith("SyntaxError")
                || lastLine.startsWith("IndentationError")
                || lastLine.startsWith("TabError");
    }

    private static Integer milliseconds(Double seconds) {
        if (seconds == null) {
            return null;
        }
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new SandboxExecutionException("Judge0 returned invalid timing data.");
        }
        double value = Math.ceil(seconds * 1000);
        if (value > Integer.MAX_VALUE) {
            throw new SandboxExecutionException("Judge0 returned timing data out of range.");
        }
        return (int) value;
    }

    private static Integer kilobytes(Double memoryKb) {
        if (memoryKb == null) {
            return null;
        }
        if (!Double.isFinite(memoryKb) || memoryKb < 0 || memoryKb > Integer.MAX_VALUE) {
            throw new SandboxExecutionException("Judge0 returned invalid memory data.");
        }
        return (int) Math.ceil(memoryKb);
    }

    private static int priority(String verdict) {
        return switch (verdict) {
            case "RUNTIME_ERROR" -> 1;
            case "MEMORY_LIMIT" -> 2;
            case "TIME_LIMIT" -> 3;
            case "WRONG_ANSWER" -> 4;
            default -> throw new SandboxExecutionException("Unknown grading verdict.");
        };
    }
}

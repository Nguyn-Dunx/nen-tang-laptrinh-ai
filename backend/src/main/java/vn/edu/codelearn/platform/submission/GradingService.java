package vn.edu.codelearn.platform.submission;

import java.util.ArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import vn.edu.codelearn.platform.sandbox.SandboxClient;
import vn.edu.codelearn.platform.sandbox.SandboxExecutionException;
import vn.edu.codelearn.platform.sandbox.SandboxLimitRepository;
import vn.edu.codelearn.platform.sandbox.SandboxRequest;

@Service
@Profile("worker")
class GradingService {

    private final GradingRepository grading;
    private final SandboxClient sandbox;
    private final SandboxLimitRepository limits;

    GradingService(
            GradingRepository grading, SandboxClient sandbox, SandboxLimitRepository limits) {
        this.grading = grading;
        this.sandbox = sandbox;
        this.limits = limits;
    }

    SubmissionViews.GradeResult grade(SubmissionViews.WorkerSubmission submission) {
        var tests = grading.findTestCases(submission.exerciseId());
        if (tests.isEmpty()) {
            throw new SandboxExecutionException("Exercise has no test cases.");
        }
        long cpuTimeLimit = limits.get("time_limit_ms");
        long wallTimeLimit = limits.get("wall_time_limit_ms");
        long cpuExtraTime = limits.get("cpu_extra_time_ms");
        long memoryLimit = limits.get("memory_limit_kb");
        long maxProcesses = limits.get("max_processes");
        long maxFileSize = limits.get("max_file_size_kb");
        long maxOutputBytes = limits.get("max_output_bytes");
        long pollTimeout = limits.get("poll_timeout_seconds");
        var results = new ArrayList<SubmissionViews.WorkerTestResult>();
        for (var test : tests) {
            var request =
                    new SandboxRequest(
                            submission.sourceCode(),
                            test.stdin(),
                            cpuTimeLimit,
                            wallTimeLimit,
                            cpuExtraTime,
                            memoryLimit,
                            maxProcesses,
                            maxFileSize,
                            maxOutputBytes,
                            pollTimeout);
            var result = GradingRules.evaluate(test, sandbox.execute(request), memoryLimit);
            results.add(result);
            if ("COMPILE_ERROR".equals(result.verdict())) {
                break;
            }
        }
        return GradingRules.aggregate(results, tests, submission.maxScore());
    }
}

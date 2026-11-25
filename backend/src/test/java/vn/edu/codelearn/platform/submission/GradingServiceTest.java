package vn.edu.codelearn.platform.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.edu.codelearn.platform.sandbox.SandboxClient;
import vn.edu.codelearn.platform.sandbox.SandboxLimitRepository;
import vn.edu.codelearn.platform.sandbox.SandboxRequest;
import vn.edu.codelearn.platform.sandbox.SandboxResult;

class GradingServiceTest {

    @Test
    void gradesAllCasesWithConfiguredLimitsAndOmitsHiddenOutput() {
        var repository = mock(GradingRepository.class);
        var sandbox = mock(SandboxClient.class);
        var limits = configuredLimits();
        when(repository.extendLease(any(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        when(repository.findTestCases(12))
                .thenReturn(
                        List.of(
                                testCase(21, 1, true, "public", "42"),
                                testCase(22, 1, false, "hidden", "secret")));
        when(sandbox.execute(any(SandboxRequest.class)))
                .thenAnswer(
                        invocation -> {
                            var request = invocation.getArgument(0, SandboxRequest.class);
                            String output = "public".equals(request.stdin()) ? "42\n" : "wrong";
                            return new SandboxResult(3, output, null, 0.02, 1024.0, false);
                        });

        var service = new GradingService(repository, sandbox, limits, 5000, 250);
        var grade =
                service.grade(
                        new SubmissionViews.WorkerSubmission(
                                1, 12, "python", "print(input())", 100, 1));

        assertThat(grade.verdict()).isEqualTo("WRONG_ANSWER");
        assertThat(grade.score()).isEqualByComparingTo("50.00");
        assertThat(grade.tests().get(0).actualOutput()).isEqualTo("42\n");
        assertThat(grade.tests().get(1).actualOutput()).isNull();
        verify(sandbox, org.mockito.Mockito.times(2)).execute(any(SandboxRequest.class));
        verify(repository, org.mockito.Mockito.times(2))
                .extendLease(any(), org.mockito.ArgumentMatchers.eq(60L));
    }

    @Test
    void stopsAfterCompileError() {
        var repository = mock(GradingRepository.class);
        var sandbox = mock(SandboxClient.class);
        var limits = configuredLimits();
        when(repository.extendLease(any(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        when(repository.findTestCases(12))
                .thenReturn(List.of(testCase(21, 1, true, "", ""), testCase(22, 1, false, "", "")));
        when(sandbox.execute(any(SandboxRequest.class)))
                .thenReturn(new SandboxResult(6, "", "SyntaxError", 0.01, 512.0, false));

        var service = new GradingService(repository, sandbox, limits, 5000, 250);
        var grade =
                service.grade(
                        new SubmissionViews.WorkerSubmission(1, 12, "python", "invalid(", 100, 1));

        assertThat(grade.verdict()).isEqualTo("COMPILE_ERROR");
        assertThat(grade.score()).isEqualByComparingTo("0.00");
        assertThat(grade.tests()).hasSize(1);
        verify(sandbox).execute(any(SandboxRequest.class));
    }

    private SandboxLimitRepository configuredLimits() {
        var limits = mock(SandboxLimitRepository.class);
        Map<String, Long> values =
                Map.of(
                        "time_limit_ms",
                        1000L,
                        "wall_time_limit_ms",
                        3000L,
                        "cpu_extra_time_ms",
                        200L,
                        "memory_limit_kb",
                        65536L,
                        "max_processes",
                        20L,
                        "max_file_size_kb",
                        1024L,
                        "max_output_bytes",
                        2048L,
                        "poll_timeout_seconds",
                        5L,
                        "stuck_timeout_seconds",
                        60L);
        when(limits.get(anyString()))
                .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        return limits;
    }

    private SubmissionViews.WorkerTestCase testCase(
            long id, int weight, boolean isPublic, String stdin, String expectedOutput) {
        return new SubmissionViews.WorkerTestCase(
                id, (int) id, stdin, expectedOutput, weight, isPublic);
    }
}

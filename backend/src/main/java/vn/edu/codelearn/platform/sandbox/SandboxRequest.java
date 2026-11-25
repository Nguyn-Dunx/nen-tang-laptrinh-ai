package vn.edu.codelearn.platform.sandbox;

public record SandboxRequest(
        String sourceCode,
        String stdin,
        long cpuTimeLimitMs,
        long wallTimeLimitMs,
        long cpuExtraTimeMs,
        long memoryLimitKb,
        long maxProcesses,
        long maxFileSizeKb,
        long maxOutputBytes,
        long pollTimeoutSeconds) {}

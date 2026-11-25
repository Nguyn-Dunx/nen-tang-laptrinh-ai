package vn.edu.codelearn.platform.sandbox;

public record SandboxResult(
        int statusId,
        String stdout,
        String stderr,
        Double timeSeconds,
        Double memoryKb,
        boolean stdoutTruncated) {}

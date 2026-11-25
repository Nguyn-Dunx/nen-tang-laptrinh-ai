package vn.edu.codelearn.platform.sandbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class Judge0Client implements SandboxClient {

    private final RestClient restClient;
    private final int pythonLanguageId;
    private final long pollIntervalMs;

    Judge0Client(RestClient restClient, int pythonLanguageId, long pollIntervalMs) {
        this.restClient = restClient;
        this.pythonLanguageId = pythonLanguageId;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    public SandboxResult execute(SandboxRequest request) {
        String token = createSubmission(request);
        long deadline =
                System.nanoTime()
                        + java.time.Duration.ofSeconds(request.pollTimeoutSeconds()).toNanos();
        while (System.nanoTime() < deadline) {
            Judge0Response response = readSubmission(token);
            if (response.status() == null || response.status().id() == null) {
                throw new SandboxExecutionException("Judge0 returned an invalid status.");
            }
            int statusId = response.status().id();
            if (statusId != 1 && statusId != 2) {
                return result(statusId, response, request.maxOutputBytes());
            }
            sleepUntilNextPoll();
        }
        throw new SandboxExecutionException("Judge0 polling timed out.");
    }

    private String createSubmission(SandboxRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source_code", request.sourceCode());
        payload.put("language_id", pythonLanguageId);
        payload.put("stdin", request.stdin());
        payload.put("cpu_time_limit", seconds(request.cpuTimeLimitMs()));
        payload.put("cpu_extra_time", seconds(request.cpuExtraTimeMs()));
        payload.put("wall_time_limit", seconds(request.wallTimeLimitMs()));
        payload.put("memory_limit", request.memoryLimitKb());
        payload.put("max_processes_and_or_threads", request.maxProcesses());
        payload.put("max_file_size", request.maxFileSizeKb());
        payload.put("enable_network", false);
        payload.put("number_of_runs", 1);
        try {
            var response =
                    restClient
                            .post()
                            .uri("/submissions?base64_encoded=false&wait=false")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(Judge0CreateResponse.class);
            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new SandboxExecutionException("Judge0 returned no submission token.");
            }
            return response.token();
        } catch (RestClientException exception) {
            throw new SandboxExecutionException("Judge0 rejected the submission request.");
        }
    }

    private Judge0Response readSubmission(String token) {
        try {
            var response =
                    restClient
                            .get()
                            .uri("/submissions/{token}?base64_encoded=false", token)
                            .retrieve()
                            .body(Judge0Response.class);
            if (response == null) {
                throw new SandboxExecutionException("Judge0 returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new SandboxExecutionException("Judge0 result could not be retrieved.");
        }
    }

    private SandboxResult result(int statusId, Judge0Response response, long maxOutputBytes) {
        String stdout = response.stdout() == null ? "" : response.stdout();
        byte[] bytes = stdout.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxOutputBytes) {
            return new SandboxResult(
                    statusId,
                    stdout,
                    diagnostic(response),
                    response.time(),
                    response.memory(),
                    false);
        }
        int limit = Math.toIntExact(Math.min(maxOutputBytes, Integer.MAX_VALUE));
        String truncated = new String(Arrays.copyOf(bytes, limit), StandardCharsets.UTF_8);
        return new SandboxResult(
                statusId,
                truncated,
                diagnostic(response),
                response.time(),
                response.memory(),
                true);
    }

    private String diagnostic(Judge0Response response) {
        if (response.stderr() != null && !response.stderr().isBlank()) {
            return response.stderr();
        }
        return response.compileOutput();
    }

    private void sleepUntilNextPoll() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SandboxExecutionException("Judge0 polling was interrupted.");
        }
    }

    private java.math.BigDecimal seconds(long milliseconds) {
        return java.math.BigDecimal.valueOf(milliseconds, 3);
    }

    private record Judge0CreateResponse(String token) {}

    private record Judge0Response(
            Judge0Status status,
            String stdout,
            String stderr,
            @JsonProperty("compile_output") String compileOutput,
            Double time,
            Double memory) {}

    private record Judge0Status(Integer id) {}
}

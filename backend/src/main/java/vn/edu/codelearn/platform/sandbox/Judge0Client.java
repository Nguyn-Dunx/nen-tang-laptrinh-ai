package vn.edu.codelearn.platform.sandbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class Judge0Client implements SandboxClient {

    private static final JsonMapper JSON =
            JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final int MAX_TOKEN_RESPONSE_BYTES = 4096;
    private static final int MAX_RESULT_RESPONSE_BYTES = 2 * 1024 * 1024;

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
        int responseLimit = resultResponseLimit(request.maxOutputBytes());
        long deadline =
                System.nanoTime()
                        + java.time.Duration.ofSeconds(request.pollTimeoutSeconds()).toNanos();
        while (System.nanoTime() < deadline) {
            Judge0Response response = readSubmission(token, responseLimit);
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
        byte[] body = JSON.writeValueAsBytes(payload);
        try {
            var response =
                    restClient
                            .post()
                            .uri("/submissions?base64_encoded=false&wait=false")
                            .contentType(MediaType.APPLICATION_JSON)
                            .contentLength(body.length)
                            .body(body)
                            .exchange(
                                    (sent, received) ->
                                            readJson(
                                                    received,
                                                    MAX_TOKEN_RESPONSE_BYTES,
                                                    Judge0CreateResponse.class));
            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new SandboxExecutionException("Judge0 returned no submission token.");
            }
            return response.token();
        } catch (RestClientException exception) {
            throw new SandboxExecutionException("Judge0 rejected the submission request.");
        }
    }

    private Judge0Response readSubmission(String token, int responseLimit) {
        try {
            var response =
                    restClient
                            .get()
                            .uri(
                                    "/submissions/{token}?base64_encoded=false"
                                            + "&fields=status,stdout,stderr,compile_output,time,memory",
                                    token)
                            .exchange(
                                    (sent, received) ->
                                            readJson(
                                                    received, responseLimit, Judge0Response.class));
            if (response == null) {
                throw new SandboxExecutionException("Judge0 returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new SandboxExecutionException("Judge0 result could not be retrieved.");
        }
    }

    private <T> T readJson(ClientHttpResponse response, int limit, Class<T> type)
            throws IOException {
        // Close the stream here so the request factory does not drain a rejected body.
        try (var input = response.getBody()) {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SandboxExecutionException("Judge0 returned an unsuccessful response.");
            }
            if (response.getHeaders().getContentLength() > limit) {
                throw new SandboxExecutionException("Judge0 response exceeded the byte limit.");
            }
            byte[] body = input.readNBytes(limit + 1);
            if (body.length > limit) {
                throw new SandboxExecutionException("Judge0 response exceeded the byte limit.");
            }
            return JSON.readValue(body, type);
        } catch (JacksonException exception) {
            throw new SandboxExecutionException("Judge0 returned invalid JSON.");
        }
    }

    private int resultResponseLimit(long maxOutputBytes) {
        // Three text fields can each need up to six JSON bytes per output byte.
        long textBudget = 18 * Math.min(maxOutputBytes, MAX_RESULT_RESPONSE_BYTES);
        return (int) Math.min(MAX_RESULT_RESPONSE_BYTES, 16384 + textBudget);
    }

    private SandboxResult result(int statusId, Judge0Response response, long maxOutputBytes) {
        String stdout = response.stdout() == null ? "" : response.stdout();
        String boundedOutput = truncUtf8(stdout, maxOutputBytes);
        return new SandboxResult(
                statusId,
                boundedOutput,
                truncUtf8(diagnostic(response), maxOutputBytes),
                response.time(),
                response.memory(),
                !boundedOutput.equals(stdout));
    }

    private String truncUtf8(String text, long limit) {
        if (text == null) {
            return null;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= limit) {
            return text;
        }
        int end = Math.toIntExact(limit);
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
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

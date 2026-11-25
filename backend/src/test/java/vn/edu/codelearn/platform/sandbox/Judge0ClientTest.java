package vn.edu.codelearn.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Judge0ClientTest {

    private HttpServer server;
    private AtomicReference<String> createBody;
    private AtomicReference<String> contentLength;
    private AtomicInteger statusPolls;
    private AtomicReference<String> transferEncoding;
    private volatile String resultOverride;
    private volatile boolean chunked;

    @BeforeEach
    void startStub() throws Exception {
        createBody = new AtomicReference<>();
        contentLength = new AtomicReference<>();
        statusPolls = new AtomicInteger();
        transferEncoding = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/submissions", exchange -> handle(exchange));
        server.start();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    @Test
    void submitsConstrainedCodeAndPollsWithoutExpectedOutputOrCallback() {
        var client =
                new SandboxWorkerConfiguration()
                        .sandboxClient(
                                "http://127.0.0.1:" + server.getAddress().getPort(),
                                71,
                                "",
                                1,
                                5000);
        var result =
                client.execute(
                        new SandboxRequest(
                                "print(input())",
                                "safe input",
                                1500,
                                5000,
                                200,
                                65536,
                                20,
                                1024,
                                2,
                                5));

        assertThat(createBody.get())
                .contains("\"language_id\":71")
                .contains("\"enable_network\":false")
                .contains("\"cpu_time_limit\":1.500")
                .doesNotContain(
                        "expected_output",
                        "callback_url",
                        "enable_per_process_and_thread_time_limit",
                        "enable_per_process_and_thread_memory_limit");
        assertThat(contentLength.get()).isNotBlank().isNotEqualTo("0");
        assertThat(transferEncoding.get()).isNull();
        assertThat(statusPolls.get()).isEqualTo(2);
        assertThat(result.stdout()).isEqualTo("5\n");
        assertThat(result.stdoutTruncated()).isTrue();
    }

    @Test
    void rejectsAnOversizedChunkedResponseBeforeDecoding() {
        chunked = true;
        resultOverride = "{\"status\":{\"id\":3},\"stderr\":\"" + "d".repeat(20000) + "\"}";

        assertThatThrownBy(this::executeSmallOutputRequest)
                .isInstanceOf(SandboxExecutionException.class)
                .hasMessage("Judge0 response exceeded the byte limit.");
    }

    @Test
    void truncatesOutputAndDiagnosticsOnUtf8Boundaries() {
        resultOverride = "{\"status\":{\"id\":3},\"stdout\":\"Aé\",\"stderr\":\"éA\"}";

        var result = executeSmallOutputRequest();

        assertThat(result.stdout()).isEqualTo("A");
        assertThat(result.stderr()).isEqualTo("é");
        assertThat(result.stdoutTruncated()).isTrue();
    }

    private SandboxResult executeSmallOutputRequest() {
        var client =
                new SandboxWorkerConfiguration()
                        .sandboxClient(
                                "http://127.0.0.1:" + server.getAddress().getPort(),
                                71,
                                "",
                                1,
                                5000);
        return client.execute(
                new SandboxRequest("print(1)", "", 1000, 3000, 200, 65536, 20, 1024, 2, 5));
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        String body;
        if ("POST".equals(exchange.getRequestMethod())) {
            createBody.set(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentLength.set(exchange.getRequestHeaders().getFirst("Content-length"));
            transferEncoding.set(exchange.getRequestHeaders().getFirst("Transfer-encoding"));
            body = "{\"token\":\"stub-token\"}";
        } else {
            int poll = statusPolls.incrementAndGet();
            body =
                    resultOverride != null
                            ? resultOverride
                            : poll < 2
                                    ? "{\"status\":{\"id\":1}}"
                                    : "{\"status\":{\"id\":3},\"stdout\":\"5\\nextra\",\"stderr\":\"\","
                                            + "\"time\":0.01,\"memory\":1024.0}";
        }
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, chunked ? 0 : response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}

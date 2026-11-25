package vn.edu.codelearn.platform.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class Judge0ClientTest {

    private HttpServer server;
    private AtomicReference<String> createBody;
    private AtomicInteger statusPolls;

    @BeforeEach
    void startStub() throws Exception {
        createBody = new AtomicReference<>();
        statusPolls = new AtomicInteger();
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
                new Judge0Client(
                        RestClient.builder()
                                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                                .requestFactory(new JdkClientHttpRequestFactory())
                                .build(),
                        71,
                        1);
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
                .doesNotContain("expected_output", "callback_url");
        assertThat(statusPolls.get()).isEqualTo(2);
        assertThat(result.stdout()).isEqualTo("5\n");
        assertThat(result.stdoutTruncated()).isTrue();
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        String body;
        if ("POST".equals(exchange.getRequestMethod())) {
            createBody.set(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            body = "{\"token\":\"stub-token\"}";
        } else {
            int poll = statusPolls.incrementAndGet();
            body =
                    poll < 2
                            ? "{\"status\":{\"id\":1}}"
                            : "{\"status\":{\"id\":3},\"stdout\":\"5\\nextra\",\"stderr\":\"\","
                                    + "\"time\":0.01,\"memory\":1024.0}";
        }
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}

package vn.edu.codelearn.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:postgresql:.+")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=${TEST_DB_URL}",
            "spring.datasource.username=${TEST_DB_USER}",
            "spring.datasource.password=${TEST_DB_PASSWORD}",
            "app.cors.allowed-origins=http://localhost:3000"
        })
@ActiveProfiles("dev")
class AuthIntegrationTest {

    private static final String PASSWORD = "test-only-long-password";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String email = "learner-" + UUID.randomUUID() + "@example.test";
    private final HttpClient client = browser();

    @LocalServerPort private int port;

    @Autowired private JdbcClient jdbc;

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void registersLearnerAndStoresRotatedSessionInPostgres() throws Exception {
        var csrf = csrf(client);
        var created =
                send(
                        client,
                        credentials("/register", email.toUpperCase(Locale.ROOT), PASSWORD)
                                .header(csrf.headerName(), csrf.value()));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).hasValue("/api/v1/auth/me");
        assertThat(sessionCookie(created)).contains("HttpOnly", "SameSite=Lax");
        assertThat(cookiePair(created)).isNotEqualTo(csrf.cookie());
        assertThat(created.body()).doesNotContain("password");

        var profile = send(client, request("/me"));
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(profile.body()).path("email").asText()).isEqualTo(email);
        assertThat(JSON.readTree(profile.body()).path("role").asText()).isEqualTo("LEARNER");
        assertThat(
                        jdbc.sql("SELECT password_hash FROM account WHERE email = :email")
                                .param("email", email)
                                .query(String.class)
                                .single())
                .startsWith("$argon2id$");
        assertThat(
                        jdbc.sql(
                                        "SELECT count(*) FROM spring_session WHERE principal_name = :email")
                                .param("email", email)
                                .query(Long.class)
                                .single())
                .isEqualTo(1L);
    }

    @Test
    void rejectsDuplicateEmailAcrossIndependentSessions() throws Exception {
        assertThat(register(client).statusCode()).isEqualTo(201);
        try (var anotherClient = browser()) {
            var csrf = csrf(anotherClient);
            var duplicate =
                    send(
                            anotherClient,
                            credentials("/register", email.toUpperCase(Locale.ROOT), PASSWORD)
                                    .header(csrf.headerName(), csrf.value()));
            assertProblem(duplicate, 409, "CONFLICT");
            assertProblem(send(anotherClient, request("/me")), 401, "UNAUTHENTICATED");
        }
    }

    @Test
    void requiresCsrfOnAuthWrites() throws Exception {
        assertProblem(send(client, credentials("/register", email, PASSWORD)), 403, "FORBIDDEN");
        var csrf = csrf(client);
        var invalid =
                send(
                        client,
                        credentials("/login", email, PASSWORD)
                                .header(csrf.headerName(), "not-a-csrf-token"));
        assertProblem(invalid, 403, "FORBIDDEN");
    }

    @Test
    void validatesRegistrationBody() throws Exception {
        var csrf = csrf(client);
        var invalid =
                send(
                        client,
                        credentials("/register", "invalid-email", PASSWORD)
                                .header(csrf.headerName(), csrf.value()));
        assertProblem(invalid, 400, "VALIDATION_ERROR");
    }

    @Test
    void validatesPasswordLimitInUnicodeCodePoints() throws Exception {
        var csrf = csrf(client);
        String character = new String(Character.toChars(0x1F512));
        var tooLong =
                send(
                        client,
                        credentials("/register", email, character.repeat(129))
                                .header(csrf.headerName(), csrf.value()));
        assertProblem(tooLong, 400, "VALIDATION_ERROR");

        var atLimit =
                send(
                        client,
                        credentials("/register", email, character.repeat(128))
                                .header(csrf.headerName(), csrf.value()));
        assertThat(atLimit.statusCode()).isEqualTo(201);
    }

    @Test
    void rejectsWrongAndUnknownCredentialsWithTheSamePublicError() throws Exception {
        assertThat(register(client).statusCode()).isEqualTo(201);
        try (var loginClient = browser()) {
            var csrf = csrf(loginClient);
            var wrongPassword =
                    send(
                            loginClient,
                            credentials("/login", email, "incorrect-password")
                                    .header(csrf.headerName(), csrf.value()));
            var unknownAccount =
                    send(
                            loginClient,
                            credentials("/login", "other-" + email, PASSWORD)
                                    .header(csrf.headerName(), csrf.value()));
            assertProblem(wrongPassword, 401, "UNAUTHENTICATED");
            assertProblem(unknownAccount, 401, "UNAUTHENTICATED");
        }
    }

    @Test
    void rotatesLoginSessionAndRevokesItOnLogout() throws Exception {
        assertThat(register(client).statusCode()).isEqualTo(201);
        try (var loginClient = browser();
                var replayClient = HttpClient.newHttpClient()) {
            var beforeLogin = csrf(loginClient);
            var login =
                    send(
                            loginClient,
                            credentials("/login", email, PASSWORD)
                                    .header(beforeLogin.headerName(), beforeLogin.value()));
            assertThat(login.statusCode()).isEqualTo(204);
            String loggedInCookie = cookiePair(login);
            assertThat(loggedInCookie).isNotEqualTo(beforeLogin.cookie());
            assertProblem(
                    send(replayClient, request("/me").header("Cookie", beforeLogin.cookie())),
                    401,
                    "UNAUTHENTICATED");
            assertThat(send(loginClient, request("/me")).statusCode()).isEqualTo(200);

            var staleToken =
                    send(
                            loginClient,
                            request("/logout")
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .header(beforeLogin.headerName(), beforeLogin.value()));
            assertProblem(staleToken, 403, "FORBIDDEN");

            var afterLogin = csrf(loginClient);
            var logout =
                    send(
                            loginClient,
                            request("/logout")
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .header(afterLogin.headerName(), afterLogin.value()));
            assertThat(logout.statusCode()).isEqualTo(204);
            assertProblem(
                    send(replayClient, request("/me").header("Cookie", loggedInCookie)),
                    401,
                    "UNAUTHENTICATED");
        }
    }

    @Test
    void permitsCredentialedCorsOnlyForConfiguredAuthOrigins() throws Exception {
        var configured = send(client, preflight("http://localhost:3000"));
        assertThat(configured.statusCode()).isEqualTo(200);
        assertThat(configured.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue("http://localhost:3000");
        assertThat(configured.headers().firstValue("Access-Control-Allow-Credentials"))
                .hasValue("true");

        var unlisted = send(client, preflight("https://example.invalid"));
        assertThat(unlisted.statusCode()).isEqualTo(403);
        assertThat(unlisted.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private HttpRequest.Builder preflight(String origin) {
        return request("/login")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type, X-CSRF-TOKEN");
    }

    private HttpResponse<String> register(HttpClient browser) throws Exception {
        var csrf = csrf(browser);
        return send(
                browser,
                credentials("/register", email, PASSWORD).header(csrf.headerName(), csrf.value()));
    }

    private Csrf csrf(HttpClient browser) throws Exception {
        var response = send(browser, request("/csrf"));
        assertThat(response.statusCode()).isEqualTo(200);
        var body = JSON.readTree(response.body());
        String cookie =
                response.headers().firstValue("Set-Cookie").isPresent() ? cookiePair(response) : "";
        return new Csrf(body.path("headerName").asText(), body.path("token").asText(), cookie);
    }

    private HttpRequest.Builder credentials(String path, String address, String password) {
        String body =
                JSON.createObjectNode().put("email", address).put("password", password).toString();
        return request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/auth" + path))
                .timeout(Duration.ofSeconds(10));
    }

    private static HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private static HttpResponse<String> send(HttpClient browser, HttpRequest.Builder request)
            throws Exception {
        return browser.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String sessionCookie(HttpResponse<?> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith("CODELEARN_SESSION="))
                .findFirst()
                .orElseThrow();
    }

    private static String cookiePair(HttpResponse<?> response) {
        return sessionCookie(response).split(";", 2)[0];
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(
                        value -> assertThat(value).startsWith("application/problem+json"));
        var body = JSON.readTree(response.body());
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(response.headers().firstValue("X-Request-Id"))
                .hasValue(body.path("traceId").asText());
    }

    private record Csrf(String headerName, String value, String cookie) {}
}

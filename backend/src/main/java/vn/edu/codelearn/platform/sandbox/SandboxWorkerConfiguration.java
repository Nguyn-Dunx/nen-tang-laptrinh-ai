package vn.edu.codelearn.platform.sandbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("worker")
@EnableScheduling
class SandboxWorkerConfiguration {

    @Bean
    SandboxClient sandboxClient(
            @Value("${judge0.base-url}") String baseUrl,
            @Value("${judge0.python-language-id}") int pythonLanguageId,
            @Value("${judge0.auth-token:}") String authToken,
            @Value("${sandbox.judge0.poll-interval-ms}") long pollIntervalMs,
            @Value("${sandbox.judge0.http-timeout-ms}") long httpTimeoutMs) {
        var httpClient =
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofMillis(httpTimeoutMs))
                        .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(java.time.Duration.ofMillis(httpTimeoutMs));
        var restClientBuilder =
                RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
        if (!authToken.isBlank()) {
            restClientBuilder.defaultHeader("X-Auth-Token", authToken);
        }
        return new Judge0Client(restClientBuilder.build(), pythonLanguageId, pollIntervalMs);
    }
}

package vn.edu.codelearn.platform.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:postgresql:.+")
@SpringBootTest(
        properties = {
            "spring.datasource.url=${TEST_DB_URL}",
            "spring.datasource.username=${TEST_DB_USER}",
            "spring.datasource.password=${TEST_DB_PASSWORD}",
            "app.cors.allowed-origins=http://localhost:3000"
        })
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class SubmissionQueueIntegrationTest {

    private static final String QUEUE_KEY = "sandbox:submissions";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcClient jdbc;
    @Autowired private StringRedisTemplate redis;

    private String email;
    private long userId;

    @BeforeEach
    void createLearner() {
        email = "sandbox-queue-" + UUID.randomUUID() + "@example.test";
        userId =
                jdbc.sql(
                                """
                        INSERT INTO account (email, password_hash, role)
                        VALUES (:email, 'test-hash', 'LEARNER')
                        RETURNING id
                        """)
                        .param("email", email)
                        .query(Long.class)
                        .single();
    }

    @AfterEach
    void removeLearnerAndRedisKeys() {
        var submissions =
                jdbc.sql("SELECT id FROM submission WHERE user_id = :userId")
                        .param("userId", userId)
                        .query(Long.class)
                        .list();
        for (Long submissionId : submissions) {
            redis.opsForList().remove(QUEUE_KEY, 0, submissionId.toString());
        }
        jdbc.sql("DELETE FROM submission WHERE user_id = :userId").param("userId", userId).update();
        jdbc.sql("DELETE FROM account WHERE id = :userId").param("userId", userId).update();
        var rateKeys = redis.keys("sandbox:rate:" + userId + ":*");
        if (rateKeys != null && !rateKeys.isEmpty()) {
            redis.delete(rateKeys);
        }
    }

    @Test
    void commitsSubmissionAndEnqueuesItsIdentifier() throws Exception {
        long exerciseId =
                jdbc.sql("SELECT id FROM exercise WHERE title = 'Tính tổng hai số'")
                        .query(Long.class)
                        .single();
        String response =
                mvc.perform(
                                post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                        .with(user(email).roles("LEARNER"))
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(
                                                """
                                                {"language":"python","source_code":"print(input())"}
                                                """))
                        .andExpect(status().isAccepted())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long submissionId =
                tools.jackson.databind.json.JsonMapper.builder()
                        .build()
                        .readTree(response)
                        .path("submission_id")
                        .asLong();

        assertThat(redis.opsForList().range(QUEUE_KEY, 0, -1))
                .contains(Long.toString(submissionId));
        assertThat(
                        jdbc.sql("SELECT status FROM submission WHERE id = :id")
                                .param("id", submissionId)
                                .query(String.class)
                                .single())
                .isEqualTo("QUEUED");
    }
}

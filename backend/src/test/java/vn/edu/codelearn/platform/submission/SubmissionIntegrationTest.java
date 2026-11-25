package vn.edu.codelearn.platform.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
@Sql({"/catalog-fixtures.sql", "/sandbox-fixtures.sql"})
@Transactional
class SubmissionIntegrationTest {

    private static final String SOURCE_CODE = "print(sum(map(int, input().split())))";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcClient jdbc;
    @Autowired private StringRedisTemplate redis;

    private String email;
    private long userId;

    @BeforeEach
    void createLearner() {
        email = "sandbox-" + UUID.randomUUID() + "@example.test";
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
            redis.opsForList().remove("sandbox:submissions", 0, submissionId.toString());
        }
        jdbc.sql("DELETE FROM submission WHERE user_id = :userId").param("userId", userId).update();
        jdbc.sql("DELETE FROM account WHERE id = :userId").param("userId", userId).update();
        var rateKeys = redis.keys("sandbox:rate:" + userId + ":*");
        if (rateKeys != null && !rateKeys.isEmpty()) {
            redis.delete(rateKeys);
        }
        resetLimit("code_size_max_bytes", 65536);
        resetLimit("submit_rate_max", 5);
        resetLimit("max_pending_per_user", 3);
    }

    @Test
    void requiresAuthenticationAndRejectsUnsupportedLanguageAndOversizedCode() throws Exception {
        long exerciseId = exerciseId();
        mvc.perform(
                        post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                .with(csrf())
                                .contentType("application/json")
                                .content(request("python", SOURCE_CODE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mvc.perform(
                        post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                .with(user(email).roles("LEARNER"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(request("java", SOURCE_CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LANGUAGE_NOT_SUPPORTED"));

        resetLimit("code_size_max_bytes", 1);
        mvc.perform(
                        post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                .with(user(email).roles("LEARNER"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(request("python", SOURCE_CODE)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("SOURCE_CODE_TOO_LARGE"));
    }

    @Test
    void acceptsQueuedSubmissionAndKeepsHiddenTestValuesPrivate() throws Exception {
        long exerciseId = exerciseId();
        MvcResult created =
                mvc.perform(
                                post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                        .with(user(email).roles("LEARNER"))
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(request("python", SOURCE_CODE)))
                        .andExpect(status().isAccepted())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.status").value("QUEUED"))
                        .andReturn();
        long submissionId = submissionId(created);

        assertPendingSubmission(submissionId);
        completeWithCanaryResults(submissionId, exerciseId);
        assertCompletedSubmissionDoesNotExposeHiddenValues(submissionId);
        assertDetailAndHistory(submissionId, exerciseId);
        assertOtherLearnerCannotRead(submissionId);
    }

    private void assertPendingSubmission(long submissionId) throws Exception {
        var pending =
                mvc.perform(
                                get("/api/v1/submissions/{submissionId}", submissionId)
                                        .with(user(email).roles("LEARNER")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("QUEUED"))
                        .andExpect(jsonPath("$.tests").isEmpty())
                        .andReturn();
        assertThat(pending.getResponse().getContentAsString()).doesNotContain("source_code");
    }

    private void assertCompletedSubmissionDoesNotExposeHiddenValues(long submissionId)
            throws Exception {
        MvcResult result =
                mvc.perform(
                                get("/api/v1/submissions/{submissionId}", submissionId)
                                        .with(user(email).roles("LEARNER")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tests.length()").value(2))
                        .andExpect(jsonPath("$.tests[0].public").value(true))
                        .andExpect(jsonPath("$.tests[0].stdin").value("public input"))
                        .andExpect(jsonPath("$.tests[1].public").value(false))
                        .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(
                        "CANARY_HIDDEN_STDIN",
                        "CANARY_HIDDEN_EXPECTED_OUTPUT",
                        "CANARY_HIDDEN_ACTUAL_OUTPUT",
                        "CANARY_REFERENCE_SOLUTION",
                        "source_code");
    }

    private void assertDetailAndHistory(long submissionId, long exerciseId) throws Exception {
        mvc.perform(
                        get("/api/v1/submissions/{submissionId}/detail", submissionId)
                                .with(user(email).roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source_code").value(SOURCE_CODE));

        MvcResult history =
                mvc.perform(
                                get("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                                        .with(user(email).roles("LEARNER")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items.length()").value(1))
                        .andReturn();
        assertThat(history.getResponse().getContentAsString()).doesNotContain("source_code");
    }

    @Test
    void returnsDifferentRateAndPendingLimitErrors() throws Exception {
        long exerciseId = exerciseId();
        resetLimit("max_pending_per_user", 1);
        submit(exerciseId).andExpect(status().isAccepted());
        submit(exerciseId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_PENDING_SUBMISSIONS"));

        jdbc.sql("DELETE FROM submission WHERE user_id = :userId").param("userId", userId).update();
        resetLimit("max_pending_per_user", 3);
        resetLimit("submit_rate_max", 1);
        var rateKeys = redis.keys("sandbox:rate:" + userId + ":*");
        if (rateKeys != null && !rateKeys.isEmpty()) {
            redis.delete(rateKeys);
        }
        submit(exerciseId).andExpect(status().isAccepted());
        submit(exerciseId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    private org.springframework.test.web.servlet.ResultActions submit(long exerciseId)
            throws Exception {
        return mvc.perform(
                post("/api/v1/exercises/{exerciseId}/submissions", exerciseId)
                        .with(user(email).roles("LEARNER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(request("python", SOURCE_CODE)));
    }

    private void assertOtherLearnerCannotRead(long submissionId) throws Exception {
        String otherEmail = "other-" + UUID.randomUUID() + "@example.test";
        jdbc.sql(
                        """
                INSERT INTO account (email, password_hash, role)
                VALUES (:email, 'test-hash', 'LEARNER')
                """)
                .param("email", otherEmail)
                .update();
        try {
            mvc.perform(
                            get("/api/v1/submissions/{submissionId}", submissionId)
                                    .with(user(otherEmail).roles("LEARNER")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SUBMISSION_NOT_FOUND"));
        } finally {
            jdbc.sql("DELETE FROM account WHERE email = :email")
                    .param("email", otherEmail)
                    .update();
        }
    }

    private void completeWithCanaryResults(long submissionId, long exerciseId) {
        jdbc.sql(
                        """
                UPDATE submission
                SET status = 'DONE', verdict = 'WRONG_ANSWER', score = 5,
                    finished_at = CURRENT_TIMESTAMP
                WHERE id = :submissionId
                """)
                .param("submissionId", submissionId)
                .update();
        var testIds =
                jdbc.sql(
                                "SELECT id FROM test_case WHERE exercise_id = :exerciseId ORDER BY position")
                        .param("exerciseId", exerciseId)
                        .query(Long.class)
                        .list();
        jdbc.sql(
                        """
                INSERT INTO submission_test_result (
                    submission_id, test_case_id, passed, verdict, actual_output
                )
                VALUES (:submissionId, :testCaseId, false, 'WRONG_ANSWER', :actualOutput)
                """)
                .param("submissionId", submissionId)
                .param("testCaseId", testIds.get(0))
                .param("actualOutput", "public actual")
                .update();
        jdbc.sql(
                        """
                INSERT INTO submission_test_result (
                    submission_id, test_case_id, passed, verdict, actual_output
                )
                VALUES (:submissionId, :testCaseId, false, 'WRONG_ANSWER', :actualOutput)
                """)
                .param("submissionId", submissionId)
                .param("testCaseId", testIds.get(1))
                .param("actualOutput", "CANARY_HIDDEN_ACTUAL_OUTPUT")
                .update();
    }

    private long exerciseId() {
        return jdbc.sql("SELECT id FROM exercise WHERE title = 'sandbox-published-exercise'")
                .query(Long.class)
                .single();
    }

    private long submissionId(MvcResult result) throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        return mapper.readTree(result.getResponse().getContentAsString())
                .path("submission_id")
                .asLong();
    }

    private String request(String language, String source) {
        return """
                {"language":"%s","source_code":"%s"}
                """
                .formatted(language, source);
    }

    private void resetLimit(String key, long value) {
        jdbc.sql("UPDATE sandbox_limit_config SET value_int = :value WHERE key = :key")
                .param("value", value)
                .param("key", key)
                .update();
    }
}

package vn.edu.codelearn.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
            "spring.datasource.password=${TEST_DB_PASSWORD}"
        })
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
@Sql({"/catalog-fixtures.sql", "/sandbox-fixtures.sql"})
class ExerciseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcClient jdbc;

    @Test
    void should_return_published_exercise_and_public_tests_without_secrets() throws Exception {
        long exerciseId = exerciseId("sandbox-published-exercise");

        MvcResult result =
                mvc.perform(get("/api/v1/exercises/{exerciseId}", exerciseId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(header().exists("X-Request-Id"))
                        .andExpect(jsonPath("$.id").value(exerciseId))
                        .andExpect(jsonPath("$.title").value("sandbox-published-exercise"))
                        .andExpect(jsonPath("$.publicTests.length()").value(1))
                        .andExpect(jsonPath("$.publicTests[0].position").value(1))
                        .andExpect(jsonPath("$.publicTests[0].stdin").value("public input"))
                        .andExpect(
                                jsonPath("$.publicTests[0].expectedOutput").value("public output"))
                        .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(
                        "CANARY_HIDDEN_STDIN",
                        "CANARY_HIDDEN_EXPECTED_OUTPUT",
                        "CANARY_REFERENCE_SOLUTION")
                .doesNotContain("hiddenTestCount", "hidden_test_count");
    }

    @Test
    void should_hide_exercises_for_draft_lesson_and_draft_course() throws Exception {
        assertExerciseNotFound("sandbox-draft-lesson-exercise");
        assertExerciseNotFound("sandbox-draft-course-exercise");
    }

    @Test
    void should_return_exercise_not_found_for_unknown_id() throws Exception {
        mvc.perform(get("/api/v1/exercises/{exerciseId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));
    }

    private void assertExerciseNotFound(String title) throws Exception {
        mvc.perform(get("/api/v1/exercises/{exerciseId}", exerciseId(title)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));
    }

    private long exerciseId(String title) {
        return jdbc.sql("SELECT id FROM exercise WHERE title = :title")
                .param("title", title)
                .query(Long.class)
                .single();
    }
}

package vn.edu.codelearn.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
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
@Transactional
@Sql("/catalog-fixtures.sql")
class CatalogIntegrationTest {

    @Autowired private MockMvc mvc;

    @Test
    void should_report_liveness_and_database_readiness() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void should_page_only_published_courses_in_slug_order() throws Exception {
        mvc.perform(get("/api/v1/courses").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("python-co-ban"));
    }

    @Test
    void should_list_ordered_published_lessons_and_serve_markdown() throws Exception {
        mvc.perform(get("/api/v1/courses/python-co-ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(2))
                .andExpect(jsonPath("$.lessons[0].slug").value("xin-chao-python"))
                .andExpect(jsonPath("$.lessons[1].slug").value("bien-va-gia-tri"));
        mvc.perform(get("/api/v1/courses/python-co-ban/lessons/xin-chao-python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseSlug").value("python-co-ban"))
                .andExpect(jsonPath("$.bodyMarkdown").isNotEmpty());
    }

    @Test
    void should_hide_drafts_even_when_a_lesson_is_published_under_a_draft_course()
            throws Exception {
        mvc.perform(get("/api/v1/courses/draft-fixture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/courses/draft-fixture/lessons/published-child"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/courses/python-co-ban/lessons/draft-child"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_a_correlatable_problem_for_invalid_pagination() throws Exception {
        mvc.perform(get("/api/v1/courses").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getContentAsString())
                                        .contains(result.getResponse().getHeader("X-Request-Id")));
    }

    @Test
    void should_deny_unlisted_routes_and_writes() throws Exception {
        mvc.perform(get("/api/v1/admin/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/v1/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void should_allow_only_configured_frontend_origins() throws Exception {
        mvc.perform(
                        options("/api/v1/courses")
                                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                        "http://localhost:3000"));
        mvc.perform(get("/api/v1/courses").header(HttpHeaders.ORIGIN, "https://example.invalid"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

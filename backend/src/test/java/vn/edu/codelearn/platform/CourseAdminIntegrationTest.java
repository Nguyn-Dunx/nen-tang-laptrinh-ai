package vn.edu.codelearn.platform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
class CourseAdminIntegrationTest {
    private static final String ADMIN = "/api/v1/admin/courses";
    private static final String COURSE = ADMIN + "/course-workflow";
    private static final String FIRST = COURSE + "/lessons/first";
    private static final String PUBLIC = "/api/v1/courses/course-workflow";
    private static final String COURSE_INPUT =
            """
            {"slug":"course-workflow","title":"  Starter course  ","summary":"An admin draft"}
            """;

    @Autowired private MockMvc mvc;

    @Test
    void adminEndpointsRequireRoleAndCsrf() throws Exception {
        mvc.perform(get(ADMIN)).andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN).with(user("learner").roles("LEARNER")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post(ADMIN)
                                .with(user("author").roles("AUTHOR"))
                                .with(csrf().asHeader())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(COURSE_INPUT))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post(ADMIN)
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(COURSE_INPUT))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminPutPreflightWithSessionAndCsrfHeader() throws Exception {
        mvc.perform(
                        options(COURSE)
                                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                        "Content-Type, X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void createsEditsAndPublishesDraftsWithoutExposingThemEarly() throws Exception {
        createCourse()
                .andExpect(header().string(HttpHeaders.LOCATION, COURSE))
                .andExpect(jsonPath("$.title").value("Starter course"));
        adminPut(
                        COURSE,
                        """
                {"title":"Updated course","summary":"Updated draft"}
                """)
                .andExpect(status().isOk());
        createLesson("first").andExpect(header().string(HttpHeaders.LOCATION, FIRST));
        adminPut(
                        FIRST,
                        """
                {"title":"Updated lesson","bodyMarkdown":"# Draft body"}
                """)
                .andExpect(status().isOk());
        adminPut(FIRST + "/publication", publication(true)).andExpect(status().isOk());
        mvc.perform(get(PUBLIC + "/lessons/first/exercises")).andExpect(status().isNotFound());
        adminPut(COURSE + "/publication", publication(true)).andExpect(status().isOk());
        mvc.perform(get(PUBLIC + "/lessons/first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated lesson"))
                .andExpect(jsonPath("$.bodyMarkdown").value("# Draft body"));
        mvc.perform(get(PUBLIC + "/lessons/first/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        adminPut(COURSE + "/publication", publication(false)).andExpect(status().isOk());
        mvc.perform(get(PUBLIC + "/lessons/first")).andExpect(status().isNotFound());
    }

    @Test
    void refusesPublishingAnEmptyCourseAndRejectsDuplicateSlugs() throws Exception {
        createCourse();
        adminPut(COURSE + "/publication", publication(true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_HAS_NO_PUBLISHED_LESSONS"));
        adminPost(ADMIN, COURSE_INPUT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_SLUG_TAKEN"));
        createLesson("first");
        adminPost(COURSE + "/lessons", lessonInput("first"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LESSON_SLUG_TAKEN"));
    }

    @Test
    void protectsPublishedContentAndItsLastPublishedLesson() throws Exception {
        createCourse();
        createLesson("first");
        adminPut(FIRST + "/publication", publication(true)).andExpect(status().isOk());
        adminPut(COURSE + "/publication", publication(true)).andExpect(status().isOk());
        adminPut(
                        COURSE,
                        """
                {"title":"Edit a published course","summary":"Conflict"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_IS_PUBLISHED"));
        adminPut(
                        FIRST,
                        """
                {"title":"Edit a published lesson","bodyMarkdown":"Conflict"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LESSON_IS_PUBLISHED"));
        adminPut(FIRST + "/publication", publication(false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_REQUIRES_PUBLISHED_LESSON"));
    }

    @Test
    void reordersLessonsAtomicallyAndRejectsAnInvalidPermutation() throws Exception {
        createCourse();
        createLesson("first");
        createLesson("second");
        adminPut(
                        COURSE + "/lesson-order",
                        """
                {"lessonSlugs":["second","first"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[0].slug").value("second"))
                .andExpect(jsonPath("$.lessons[1].position").value(2));
        adminPut(
                        COURSE + "/lesson-order",
                        """
                {"lessonSlugs":["first","first"]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LESSON_ORDER"));
        mvc.perform(get(COURSE).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[0].slug").value("second"));
    }

    @Test
    void rejectsInvalidSlugAndMissingPublicationFlag() throws Exception {
        adminPost(
                        ADMIN,
                        """
                {"slug":"Upper case","title":"Title","summary":"Summary"}
                """)
                .andExpect(status().isBadRequest());
        createCourse();
        adminPut(COURSE + "/publication", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void listsDraftsForAdminButNotForGuests() throws Exception {
        createCourse();
        mvc.perform(get(ADMIN).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("course-workflow"));
        mvc.perform(get(PUBLIC)).andExpect(status().isNotFound());
    }

    private ResultActions createCourse() throws Exception {
        return adminPost(ADMIN, COURSE_INPUT).andExpect(status().isCreated());
    }

    private ResultActions createLesson(String slug) throws Exception {
        return adminPost(COURSE + "/lessons", lessonInput(slug)).andExpect(status().isCreated());
    }

    private String lessonInput(String slug) {
        return """
                {"slug":"%s","title":"First lesson","bodyMarkdown":"# First lesson"}
                """
                .formatted(slug);
    }

    private String publication(boolean published) {
        return "{\"published\":" + published + "}";
    }

    private ResultActions adminPost(String url, String body) throws Exception {
        return mvc.perform(
                post(url)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions adminPut(String url, String body) throws Exception {
        return mvc.perform(
                put(url).with(user("admin").roles("ADMIN"))
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }
}

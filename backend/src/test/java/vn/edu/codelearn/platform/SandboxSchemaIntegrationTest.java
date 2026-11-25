package vn.edu.codelearn.platform;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:postgresql:.+")
@SpringBootTest(
        properties = {
            "spring.datasource.url=${TEST_DB_URL}",
            "spring.datasource.username=${TEST_DB_USER}",
            "spring.datasource.password=${TEST_DB_PASSWORD}"
        })
@ActiveProfiles("dev")
@Transactional
@Sql({"/catalog-fixtures.sql", "/sandbox-fixtures.sql"})
class SandboxSchemaIntegrationTest {

    @Autowired private JdbcClient jdbc;

    @Test
    void rejectsNonPositiveTestWeight() {
        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                """
                                                INSERT INTO test_case (
                                                    exercise_id, position, stdin, expected_output, is_public, weight
                                                )
                                                SELECT id, 3, '', '', false, 0
                                                FROM exercise WHERE title = 'sandbox-published-exercise'
                                                """)
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateTestPositionsForAnExercise() {
        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                """
                                                INSERT INTO test_case (
                                                    exercise_id, position, stdin, expected_output, is_public, weight
                                                )
                                                SELECT id, 1, '', '', false, 1
                                                FROM exercise WHERE title = 'sandbox-published-exercise'
                                                """)
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDoneSubmissionsWithoutAVerdict() {
        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                """
                                                INSERT INTO submission (user_id, exercise_id, language, source_code, status)
                                                SELECT account.id, exercise.id, 'python', '', 'DONE'
                                                FROM account CROSS JOIN exercise
                                                WHERE account.email = 'sandbox-fixture@example.test'
                                                  AND exercise.title = 'sandbox-published-exercise'
                                                """)
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

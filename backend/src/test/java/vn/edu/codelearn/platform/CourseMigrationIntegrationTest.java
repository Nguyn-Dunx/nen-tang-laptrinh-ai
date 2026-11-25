package vn.edu.codelearn.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:postgresql:.+")
class CourseMigrationIntegrationTest {

    @Test
    void upgradesAnExistingCatalogAndExerciseWithoutChangingTheirIds() {
        var dataSource =
                new DriverManagerDataSource(
                        System.getenv("TEST_DB_URL"),
                        System.getenv("TEST_DB_USER"),
                        System.getenv("TEST_DB_PASSWORD"));
        String schema = "course_migration_" + UUID.randomUUID().toString().replace("-", "");
        dataSource.setSchema(schema);
        var latest = configure(dataSource, schema).cleanDisabled(false).load();
        var jdbc = JdbcClient.create(dataSource);
        try {
            configure(dataSource, schema).target("8").load().migrate();
            long exerciseId = insertLegacyCatalog(jdbc);
            latest.migrate();

            long migratedExerciseId =
                    jdbc.sql(
                                    """
                    SELECT exercise.id FROM exercise
                    JOIN published_lesson ON published_lesson.id = exercise.lesson_id
                    WHERE course_slug = 'legacy-course' AND slug = 'legacy-lesson'
                    """)
                            .query(Long.class)
                            .single();
            assertThat(migratedExerciseId).isEqualTo(exerciseId);
        } finally {
            latest.clean();
        }
    }

    private long insertLegacyCatalog(JdbcClient jdbc) {
        return jdbc.sql(
                        """
                WITH existing_course AS (
                    INSERT INTO course (slug, title, summary, published_at)
                    VALUES ('legacy-course', 'Legacy course', 'Summary', CURRENT_TIMESTAMP) RETURNING id
                ), existing_lesson AS (
                    INSERT INTO lesson (course_id, slug, title, position, body_markdown, published_at)
                    SELECT id, 'legacy-lesson', 'Legacy lesson', 1, 'Markdown', CURRENT_TIMESTAMP
                    FROM existing_course RETURNING id
                )
                INSERT INTO exercise (lesson_id, title, language, difficulty, statement_md,
                    starter_code, grading_type, max_score)
                SELECT id, 'Legacy exercise', 'python', 'beginner', 'Statement', '', 'OUTPUT_MATCH', 10
                FROM existing_lesson RETURNING id
                """)
                .query(Long.class)
                .single();
    }

    private FluentConfiguration configure(DriverManagerDataSource dataSource, String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration");
    }
}

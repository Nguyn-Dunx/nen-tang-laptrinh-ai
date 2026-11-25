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
class SandboxMigrationIntegrationTest {

    @Test
    void upgradesV6WithoutDevSeedAndPreservesConfiguredLimits() {
        var dataSource =
                new DriverManagerDataSource(
                        System.getenv("TEST_DB_URL"),
                        System.getenv("TEST_DB_USER"),
                        System.getenv("TEST_DB_PASSWORD"));
        String schema = "sandbox_migration_" + UUID.randomUUID().toString().replace("-", "");
        dataSource.setSchema(schema);
        var jdbc = JdbcClient.create(dataSource);
        var migration = configuration(dataSource, schema).cleanDisabled(false).load();
        try {
            configuration(dataSource, schema).target("6").load().migrate();
            jdbc.sql(
                            """
                    INSERT INTO sandbox_limit_config(key, value_int, unit)
                    VALUES ('submit_rate_max', 12, 'count')
                    """)
                    .update();

            migration.migrate();
            assertSandboxDefaults(jdbc);
        } finally {
            migration.clean();
        }
    }

    private FluentConfiguration configuration(DriverManagerDataSource dataSource, String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration");
    }

    private void assertSandboxDefaults(JdbcClient jdbc) {
        assertThat(jdbc.sql("SELECT count(*) FROM sandbox_limit_config").query(Long.class).single())
                .isEqualTo(16);
        assertThat(
                        jdbc.sql(
                                        """
                    SELECT value_int FROM sandbox_limit_config WHERE key = 'submit_rate_max'
                    """)
                                .query(Long.class)
                                .single())
                .isEqualTo(12);
    }
}

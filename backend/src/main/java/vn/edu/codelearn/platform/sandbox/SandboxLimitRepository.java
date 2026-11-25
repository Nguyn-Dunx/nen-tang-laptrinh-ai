package vn.edu.codelearn.platform.sandbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SandboxLimitRepository {

    private final JdbcClient jdbc;

    public SandboxLimitRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long get(String key) {
        return jdbc.sql("SELECT value_int FROM sandbox_limit_config WHERE key = :key")
                .param("key", key)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new SandboxConfigurationException(key));
    }
}

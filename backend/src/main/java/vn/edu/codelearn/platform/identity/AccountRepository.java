package vn.edu.codelearn.platform.identity;

import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepository {

    private static final RowMapper<Account> ACCOUNT =
            (row, index) ->
                    new Account(
                            row.getLong("id"),
                            row.getString("email"),
                            row.getString("password_hash"),
                            Account.Role.valueOf(row.getString("role")));

    private final JdbcClient jdbc;

    AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Account> insertLearner(String email, String passwordHash) {
        return jdbc.sql(
                        """
                INSERT INTO account (email, password_hash, role)
                VALUES (:email, :passwordHash, 'LEARNER')
                ON CONFLICT (email) DO NOTHING
                RETURNING id, email, password_hash, role
                """)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .query(ACCOUNT)
                .optional();
    }

    Optional<Account> findByEmail(String email) {
        return jdbc.sql("SELECT id, email, password_hash, role FROM account WHERE email = :email")
                .param("email", email)
                .query(ACCOUNT)
                .optional();
    }
}

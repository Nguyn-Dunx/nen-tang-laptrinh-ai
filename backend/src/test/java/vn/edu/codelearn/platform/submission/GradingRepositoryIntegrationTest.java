package vn.edu.codelearn.platform.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.codelearn.platform.sandbox.SandboxLimitRepository;
import vn.edu.codelearn.platform.sandbox.SandboxResult;

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
class GradingRepositoryIntegrationTest {

    @Autowired private GradingRepository grading;
    @Autowired private JdbcClient jdbc;
    @Autowired private SandboxLimitRepository limits;

    @Test
    void rejectsARecoverySnapshotAfterTheActiveWorkerRenewsItsLease() {
        long id = fixtureSubmissionId();
        var first = grading.claim(id).orElseThrow();
        assertThat(grading.claim(id)).isEmpty();
        expireLease(id);
        var stale =
                grading.findStuck(60, 100).stream()
                        .filter(item -> item.id() == id)
                        .findFirst()
                        .orElseThrow();

        assertThat(grading.extendLease(first, 120)).isTrue();
        assertThat(grading.recoverStuck(stale, 60, 3, 0)).isFalse();
        assertThat(grading.findStuck(60, 100)).noneMatch(item -> item.id() == id);

        expireLease(id);
        assertThat(grading.recoverStuck(stale, 60, 3, 0)).isTrue();
        var second = grading.claim(id).orElseThrow();
        assertThat(second.attempt()).isEqualTo(first.attempt() + 1);
        assertThat(grading.extendLease(first, 120)).isFalse();
    }

    @Test
    void workerClaimsGradesAndPersistsPublicAndHiddenResultsWithAStub() {
        long id = fixtureSubmissionId();
        var queue = mock(SubmissionQueue.class);
        when(queue.dequeue()).thenReturn(id);
        var grader =
                new GradingService(
                        grading,
                        request ->
                                new SandboxResult(
                                        3,
                                        "public input".equals(request.stdin())
                                                ? "public output"
                                                : "wrong",
                                        null,
                                        0.01,
                                        1024.0,
                                        false),
                        limits,
                        5000,
                        250);

        new SubmissionWorker(queue, grading, grader, limits).processNext();

        assertPersistedGrade(id);
    }

    private void assertPersistedGrade(long id) {
        assertThat(
                        jdbc.sql(
                                        "SELECT verdict FROM submission WHERE id = :id AND status = 'DONE'")
                                .param("id", id)
                                .query(String.class)
                                .single())
                .isEqualTo("WRONG_ANSWER");
        assertThat(
                        jdbc.sql(
                                        """
                SELECT result.actual_output
                FROM submission_test_result result
                JOIN test_case ON test_case.id = result.test_case_id
                WHERE result.submission_id = :id AND test_case.is_public
                """)
                                .param("id", id)
                                .query(String.class)
                                .single())
                .isEqualTo("public output");
        assertThat(
                        jdbc.sql(
                                        """
                SELECT count(*)
                FROM submission_test_result result
                JOIN test_case ON test_case.id = result.test_case_id
                WHERE result.submission_id = :id AND NOT test_case.is_public
                  AND result.actual_output IS NULL
                """)
                                .param("id", id)
                                .query(Long.class)
                                .single())
                .isEqualTo(1);
    }

    private long fixtureSubmissionId() {
        return jdbc.sql(
                        """
                SELECT submission.id FROM submission
                JOIN account ON account.id = submission.user_id
                JOIN exercise ON exercise.id = submission.exercise_id
                WHERE account.email = 'sandbox-fixture@example.test'
                  AND exercise.title = 'sandbox-published-exercise'
                """)
                .query(Long.class)
                .single();
    }

    private void expireLease(long id) {
        jdbc.sql(
                        """
                UPDATE submission SET lease_expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = :id
                """)
                .param("id", id)
                .update();
    }
}

package vn.edu.codelearn.platform.sandbox;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ExerciseRepository {

    private static final RowMapper<ExerciseViews.ExerciseRow> EXERCISE_ROW =
            (row, index) ->
                    new ExerciseViews.ExerciseRow(
                            row.getLong("id"),
                            row.getLong("lesson_id"),
                            row.getString("title"),
                            row.getString("language"),
                            row.getString("difficulty"),
                            row.getString("statement_md"),
                            row.getString("starter_code"),
                            row.getInt("max_score"));

    private final JdbcClient jdbc;

    ExerciseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<ExerciseViews.ExerciseRow> findPublishedExercise(long exerciseId) {
        return jdbc.sql(
                        """
                SELECT exercise.id, exercise.lesson_id, exercise.title, exercise.language,
                       exercise.difficulty, exercise.statement_md, exercise.starter_code,
                       exercise.max_score
                FROM exercise
                JOIN published_lesson lesson ON lesson.id = exercise.lesson_id
                WHERE exercise.id = :exerciseId
                """)
                .param("exerciseId", exerciseId)
                .query(EXERCISE_ROW)
                .optional();
    }

    List<ExerciseCatalog.ExerciseSummary> findForPublishedLesson(long lessonId) {
        return jdbc.sql(
                        """
                SELECT exercise.id, exercise.title, exercise.language FROM exercise
                JOIN published_lesson lesson ON lesson.id = exercise.lesson_id
                WHERE lesson.id = :lessonId ORDER BY exercise.id
                """)
                .param("lessonId", lessonId)
                .query(
                        (row, index) ->
                                new ExerciseCatalog.ExerciseSummary(
                                        row.getLong("id"),
                                        row.getString("title"),
                                        row.getString("language")))
                .list();
    }

    List<ExerciseViews.PublicTestCase> findPublicTestCases(long exerciseId) {
        return jdbc.sql(
                        """
                SELECT position, stdin, expected_output
                FROM test_case_public
                WHERE exercise_id = :exerciseId
                ORDER BY position
                """)
                .param("exerciseId", exerciseId)
                .query(
                        (row, index) ->
                                new ExerciseViews.PublicTestCase(
                                        row.getInt("position"),
                                        row.getString("stdin"),
                                        row.getString("expected_output")))
                .list();
    }
}

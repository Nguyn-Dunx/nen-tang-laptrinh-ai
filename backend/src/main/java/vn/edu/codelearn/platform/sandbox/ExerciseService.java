package vn.edu.codelearn.platform.sandbox;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.codelearn.platform.common.ApiException;

@Service
class ExerciseService implements ExerciseCatalog {

    private final ExerciseRepository exercises;

    ExerciseService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseSummary> listForPublishedLesson(long lessonId) {
        return exercises.findForPublishedLesson(lessonId);
    }

    @Transactional(readOnly = true)
    ExerciseViews.ExerciseView getExercise(long exerciseId) {
        var exercise =
                exercises
                        .findPublishedExercise(exerciseId)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND"));
        return new ExerciseViews.ExerciseView(
                exercise.id(),
                exercise.lessonId(),
                exercise.title(),
                exercise.language(),
                exercise.difficulty(),
                exercise.statementMarkdown(),
                exercise.starterCode(),
                exercise.maxScore(),
                exercises.findPublicTestCases(exercise.id()));
    }
}

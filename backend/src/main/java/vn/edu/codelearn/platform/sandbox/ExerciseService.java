package vn.edu.codelearn.platform.sandbox;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.codelearn.platform.common.ApiException;

@Service
class ExerciseService {

    private final ExerciseRepository exercises;

    ExerciseService(ExerciseRepository exercises) {
        this.exercises = exercises;
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

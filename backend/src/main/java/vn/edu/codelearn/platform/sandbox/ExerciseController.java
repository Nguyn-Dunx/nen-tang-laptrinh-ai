package vn.edu.codelearn.platform.sandbox;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
class ExerciseController {

    private final ExerciseService exercises;

    ExerciseController(ExerciseService exercises) {
        this.exercises = exercises;
    }

    @GetMapping("/{exerciseId}")
    ExerciseViews.ExerciseView getExercise(@PathVariable long exerciseId) {
        return exercises.getExercise(exerciseId);
    }
}

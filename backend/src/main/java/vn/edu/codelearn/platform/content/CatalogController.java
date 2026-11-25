package vn.edu.codelearn.platform.content;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.codelearn.platform.sandbox.ExerciseCatalog;

@RestController
@RequestMapping("/api/v1/courses")
class CatalogController {

    private final CatalogService catalog;

    CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    CatalogViews.CoursePage listCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return catalog.listCourses(page, size);
    }

    @GetMapping("/{courseSlug}")
    CatalogViews.CourseDetail getCourse(@PathVariable String courseSlug) {
        return catalog.getCourse(courseSlug);
    }

    @GetMapping("/{courseSlug}/lessons/{lessonSlug}")
    CatalogViews.LessonDetail getLesson(
            @PathVariable String courseSlug, @PathVariable String lessonSlug) {
        return catalog.getLesson(courseSlug, lessonSlug);
    }

    @GetMapping("/{courseSlug}/lessons/{lessonSlug}/exercises")
    List<ExerciseCatalog.ExerciseSummary> listLessonExercises(
            @PathVariable String courseSlug, @PathVariable String lessonSlug) {
        return catalog.listLessonExercises(courseSlug, lessonSlug);
    }
}

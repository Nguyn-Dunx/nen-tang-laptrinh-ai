package vn.edu.codelearn.platform.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CourseAdminController.BASE)
class CourseAdminController {

    static final String BASE = "/api/v1/admin/courses";
    private static final String COURSE = "/{courseSlug}";
    private static final String LESSONS = COURSE + "/lessons";
    private static final String LESSON = LESSONS + "/{lessonSlug}";

    private final CourseAdminService courses;

    CourseAdminController(CourseAdminService courses) {
        this.courses = courses;
    }

    @GetMapping
    CourseAdminDtos.CoursePage listCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return courses.listCourses(page, size);
    }

    @PostMapping
    ResponseEntity<CourseAdminDtos.CourseDetail> createCourse(
            @Valid @RequestBody CourseAdminDtos.CreateCourse request) {
        var course = courses.createCourse(request);
        return ResponseEntity.created(URI.create(BASE + "/" + course.slug())).body(course);
    }

    @GetMapping(COURSE)
    CourseAdminDtos.CourseDetail getCourse(@PathVariable String courseSlug) {
        return courses.getCourse(courseSlug);
    }

    @PutMapping(COURSE)
    CourseAdminDtos.CourseDetail updateCourse(
            @PathVariable String courseSlug,
            @Valid @RequestBody CourseAdminDtos.UpdateCourse request) {
        return courses.updateCourse(courseSlug, request);
    }

    @PutMapping(COURSE + "/publication")
    CourseAdminDtos.CourseDetail publishCourse(
            @PathVariable String courseSlug,
            @Valid @RequestBody CourseAdminDtos.Publication request) {
        return courses.publishCourse(courseSlug, request.published());
    }

    @PostMapping(LESSONS)
    ResponseEntity<CourseAdminDtos.LessonDetail> createLesson(
            @PathVariable String courseSlug,
            @Valid @RequestBody CourseAdminDtos.CreateLesson request) {
        var lesson = courses.createLesson(courseSlug, request);
        String location = BASE + "/" + courseSlug + "/lessons/" + lesson.slug();
        return ResponseEntity.created(URI.create(location)).body(lesson);
    }

    @GetMapping(LESSON)
    CourseAdminDtos.LessonDetail getLesson(
            @PathVariable String courseSlug, @PathVariable String lessonSlug) {
        return courses.getLesson(courseSlug, lessonSlug);
    }

    @PutMapping(LESSON)
    CourseAdminDtos.LessonDetail updateLesson(
            @PathVariable String courseSlug,
            @PathVariable String lessonSlug,
            @Valid @RequestBody CourseAdminDtos.UpdateLesson request) {
        return courses.updateLesson(courseSlug, lessonSlug, request);
    }

    @PutMapping(LESSON + "/publication")
    CourseAdminDtos.LessonDetail publishLesson(
            @PathVariable String courseSlug,
            @PathVariable String lessonSlug,
            @Valid @RequestBody CourseAdminDtos.Publication request) {
        return courses.publishLesson(courseSlug, lessonSlug, request.published());
    }

    @PutMapping(COURSE + "/lesson-order")
    CourseAdminDtos.CourseDetail orderLessons(
            @PathVariable String courseSlug,
            @Valid @RequestBody CourseAdminDtos.LessonOrder request) {
        return courses.orderLessons(courseSlug, request.lessonSlugs());
    }
}

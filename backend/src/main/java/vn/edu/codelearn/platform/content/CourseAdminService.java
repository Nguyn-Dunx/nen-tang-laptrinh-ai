package vn.edu.codelearn.platform.content;

import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.codelearn.platform.common.ApiException;

@Service
class CourseAdminService {

    private final CourseAdminRepository courses;

    CourseAdminService(CourseAdminRepository courses) {
        this.courses = courses;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    CourseAdminDtos.CoursePage listCourses(int page, int size) {
        var items =
                courses.listCourses(size, (long) page * size).stream()
                        .map(CourseAdminDtos.CourseItem::from)
                        .toList();
        return new CourseAdminDtos.CoursePage(items, page, size, courses.countCourses());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    CourseAdminDtos.CourseDetail getCourse(String slug) {
        return detail(findCourse(slug));
    }

    @Transactional
    CourseAdminDtos.CourseDetail createCourse(CourseAdminDtos.CreateCourse request) {
        return detail(
                courses.insertCourse(request).orElseThrow(() -> conflict("COURSE_SLUG_TAKEN")));
    }

    @Transactional
    CourseAdminDtos.CourseDetail updateCourse(String slug, CourseAdminDtos.UpdateCourse request) {
        var course = lockCourse(slug);
        if (course.publishedAt() != null) {
            throw conflict("COURSE_IS_PUBLISHED");
        }
        return detail(courses.updateCourse(course.id(), request));
    }

    @Transactional
    CourseAdminDtos.CourseDetail publishCourse(String slug, boolean published) {
        var course = lockCourse(slug);
        if ((course.publishedAt() != null) == published) {
            return detail(course);
        }
        if (published && courses.countPublishedLessons(course.id()) == 0) {
            throw conflict("COURSE_HAS_NO_PUBLISHED_LESSONS");
        }
        return detail(courses.setCoursePublication(course.id(), published));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    CourseAdminDtos.LessonDetail getLesson(String courseSlug, String lessonSlug) {
        return findLesson(findCourse(courseSlug).id(), lessonSlug);
    }

    @Transactional
    CourseAdminDtos.LessonDetail createLesson(
            String courseSlug, CourseAdminDtos.CreateLesson request) {
        var course = lockCourse(courseSlug);
        return courses.insertLesson(course.id(), request)
                .orElseThrow(() -> conflict("LESSON_SLUG_TAKEN"));
    }

    @Transactional
    CourseAdminDtos.LessonDetail updateLesson(
            String courseSlug, String lessonSlug, CourseAdminDtos.UpdateLesson request) {
        var course = lockCourse(courseSlug);
        if (findLesson(course.id(), lessonSlug).publishedAt() != null) {
            throw conflict("LESSON_IS_PUBLISHED");
        }
        return courses.updateLesson(course.id(), lessonSlug, request);
    }

    @Transactional
    CourseAdminDtos.LessonDetail publishLesson(
            String courseSlug, String lessonSlug, boolean published) {
        var course = lockCourse(courseSlug);
        var lesson = findLesson(course.id(), lessonSlug);
        if ((lesson.publishedAt() != null) == published) {
            return lesson;
        }
        if (!published
                && course.publishedAt() != null
                && courses.countPublishedLessons(course.id()) == 1) {
            throw conflict("COURSE_REQUIRES_PUBLISHED_LESSON");
        }
        return courses.setLessonPublication(course.id(), lessonSlug, published);
    }

    @Transactional
    CourseAdminDtos.CourseDetail orderLessons(String courseSlug, List<String> slugs) {
        // All lesson writers lock the parent so reordering and appending cannot race.
        var course = lockCourse(courseSlug);
        var current =
                courses.listLessons(course.id()).stream()
                        .map(CourseAdminDtos.LessonItem::slug)
                        .toList();
        if (slugs.size() != current.size()
                || !new HashSet<>(slugs).equals(new HashSet<>(current))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LESSON_ORDER");
        }
        courses.orderLessons(course.id(), slugs);
        return detail(course);
    }

    private CourseAdminDtos.CourseRow findCourse(String slug) {
        return courses.findCourse(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND"));
    }

    private CourseAdminDtos.CourseRow lockCourse(String slug) {
        return courses.lockCourse(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND"));
    }

    private CourseAdminDtos.LessonDetail findLesson(long courseId, String slug) {
        return courses.findLesson(courseId, slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND"));
    }

    private CourseAdminDtos.CourseDetail detail(CourseAdminDtos.CourseRow course) {
        return new CourseAdminDtos.CourseDetail(
                course.slug(),
                course.title(),
                course.summary(),
                course.publishedAt(),
                courses.listLessons(course.id()));
    }

    private ApiException conflict(String code) {
        return new ApiException(HttpStatus.CONFLICT, code);
    }
}

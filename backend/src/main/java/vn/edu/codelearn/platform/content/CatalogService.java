package vn.edu.codelearn.platform.content;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CatalogService {

    private final CatalogRepository catalog;

    CatalogService(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    CatalogViews.CoursePage listCourses(int page, int size) {
        long offset = (long) page * size;
        return new CatalogViews.CoursePage(
                catalog.findPublishedCourses(size, offset), page, size, catalog.countPublishedCourses());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    CatalogViews.CourseDetail getCourse(String slug) {
        var course = catalog.findPublishedCourse(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new CatalogViews.CourseDetail(
                course.slug(), course.title(), course.summary(), catalog.findPublishedLessons(slug));
    }

    CatalogViews.LessonDetail getLesson(String courseSlug, String lessonSlug) {
        return catalog.findPublishedLesson(courseSlug, lessonSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

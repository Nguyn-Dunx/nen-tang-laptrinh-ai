package vn.edu.codelearn.platform.content;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class CatalogRepository {

    private static final RowMapper<CatalogViews.CourseSummary> COURSE_SUMMARY = (row, index) ->
            new CatalogViews.CourseSummary(row.getString("slug"), row.getString("title"), row.getString("summary"));

    private final JdbcClient jdbc;

    CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<CatalogViews.CourseSummary> findPublishedCourses(int size, long offset) {
        return jdbc.sql("""
                SELECT slug, title, summary
                FROM course
                WHERE published_at IS NOT NULL
                ORDER BY slug
                LIMIT :size OFFSET :offset
                """)
                .param("size", size)
                .param("offset", offset)
                .query(COURSE_SUMMARY)
                .list();
    }

    long countPublishedCourses() {
        return jdbc.sql("SELECT COUNT(*) FROM course WHERE published_at IS NOT NULL")
                .query(Long.class)
                .single();
    }

    Optional<CatalogViews.CourseSummary> findPublishedCourse(String slug) {
        return jdbc.sql("""
                SELECT slug, title, summary
                FROM course
                WHERE slug = :slug AND published_at IS NOT NULL
                """)
                .param("slug", slug)
                .query(COURSE_SUMMARY)
                .optional();
    }

    List<CatalogViews.LessonSummary> findPublishedLessons(String courseSlug) {
        return jdbc.sql("""
                SELECT lesson.slug, lesson.title, lesson.position
                FROM lesson
                JOIN course ON course.id = lesson.course_id
                WHERE course.slug = :courseSlug AND course.published_at IS NOT NULL
                    AND lesson.published_at IS NOT NULL
                ORDER BY lesson.position
                """)
                .param("courseSlug", courseSlug)
                .query((row, index) -> new CatalogViews.LessonSummary(
                        row.getString("slug"), row.getString("title"), row.getInt("position")))
                .list();
    }

    Optional<CatalogViews.LessonDetail> findPublishedLesson(String courseSlug, String lessonSlug) {
        return jdbc.sql("""
                SELECT course.slug AS course_slug, lesson.slug, lesson.title, lesson.position, lesson.body_markdown
                FROM lesson
                JOIN course ON course.id = lesson.course_id
                WHERE course.slug = :courseSlug AND lesson.slug = :lessonSlug
                    AND course.published_at IS NOT NULL AND lesson.published_at IS NOT NULL
                """)
                .param("courseSlug", courseSlug)
                .param("lessonSlug", lessonSlug)
                .query((row, index) -> new CatalogViews.LessonDetail(
                        row.getString("course_slug"), row.getString("slug"), row.getString("title"),
                        row.getInt("position"), row.getString("body_markdown")))
                .optional();
    }
}

package vn.edu.codelearn.platform.content;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class CatalogRepository {

    private static final RowMapper<CatalogViews.CourseSummary> COURSE_SUMMARY =
            (row, index) ->
                    new CatalogViews.CourseSummary(
                            row.getString("slug"),
                            row.getString("title"),
                            row.getString("summary"));

    private final JdbcClient jdbc;

    CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<CatalogViews.CourseSummary> findPublishedCourses(int size, long offset) {
        return jdbc.sql(
                        """
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
        return jdbc.sql(
                        """
                SELECT slug, title, summary
                FROM course
                WHERE slug = :slug AND published_at IS NOT NULL
                """)
                .param("slug", slug)
                .query(COURSE_SUMMARY)
                .optional();
    }

    List<CatalogViews.LessonSummary> findPublishedLessons(String courseSlug) {
        return jdbc.sql(
                        """
                SELECT slug, title, position
                FROM published_lesson
                WHERE course_slug = :courseSlug
                ORDER BY position
                """)
                .param("courseSlug", courseSlug)
                .query(
                        (row, index) ->
                                new CatalogViews.LessonSummary(
                                        row.getString("slug"),
                                        row.getString("title"),
                                        row.getInt("position")))
                .list();
    }

    Optional<CatalogViews.LessonDetail> findPublishedLesson(String courseSlug, String lessonSlug) {
        return jdbc.sql(
                        """
                WITH ordered_lessons AS (
                    SELECT course_slug, slug, title, position, body_markdown,
                           lag(slug) OVER (ORDER BY position) AS previous_slug,
                           lead(slug) OVER (ORDER BY position) AS next_slug
                    FROM published_lesson
                    WHERE course_slug = :courseSlug
                )
                SELECT * FROM ordered_lessons WHERE slug = :lessonSlug
                """)
                .param("courseSlug", courseSlug)
                .param("lessonSlug", lessonSlug)
                .query(
                        (row, index) ->
                                new CatalogViews.LessonDetail(
                                        row.getString("course_slug"),
                                        row.getString("slug"),
                                        row.getString("title"),
                                        row.getInt("position"),
                                        row.getString("body_markdown"),
                                        row.getString("previous_slug"),
                                        row.getString("next_slug")))
                .optional();
    }

    Optional<Long> findPublishedLessonId(String courseSlug, String lessonSlug) {
        return jdbc.sql(
                        """
                SELECT id FROM published_lesson WHERE course_slug = :courseSlug AND slug = :lessonSlug
                """)
                .param("courseSlug", courseSlug)
                .param("lessonSlug", lessonSlug)
                .query(Long.class)
                .optional();
    }
}

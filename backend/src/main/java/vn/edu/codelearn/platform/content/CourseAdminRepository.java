package vn.edu.codelearn.platform.content;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class CourseAdminRepository {

    private static final RowMapper<CourseAdminDtos.CourseRow> COURSE =
            (row, index) ->
                    new CourseAdminDtos.CourseRow(
                            row.getLong("id"),
                            row.getString("slug"),
                            row.getString("title"),
                            row.getString("summary"),
                            row.getObject("published_at", OffsetDateTime.class));
    private static final RowMapper<CourseAdminDtos.LessonDetail> LESSON =
            (row, index) ->
                    new CourseAdminDtos.LessonDetail(
                            row.getString("slug"),
                            row.getString("title"),
                            row.getInt("position"),
                            row.getString("body_markdown"),
                            row.getObject("published_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    CourseAdminRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<CourseAdminDtos.CourseRow> listCourses(int size, long offset) {
        return jdbc.sql(
                        """
                SELECT id, slug, title, summary, published_at FROM course
                ORDER BY slug LIMIT :size OFFSET :offset
                """)
                .param("size", size)
                .param("offset", offset)
                .query(COURSE)
                .list();
    }

    long countCourses() {
        return jdbc.sql("SELECT count(*) FROM course").query(Long.class).single();
    }

    Optional<CourseAdminDtos.CourseRow> findCourse(String slug) {
        return jdbc.sql(
                        """
                SELECT id, slug, title, summary, published_at FROM course WHERE slug = :slug
                """)
                .param("slug", slug)
                .query(COURSE)
                .optional();
    }

    Optional<CourseAdminDtos.CourseRow> lockCourse(String slug) {
        return jdbc.sql(
                        """
                SELECT id, slug, title, summary, published_at FROM course WHERE slug = :slug FOR UPDATE
                """)
                .param("slug", slug)
                .query(COURSE)
                .optional();
    }

    Optional<CourseAdminDtos.CourseRow> insertCourse(CourseAdminDtos.CreateCourse input) {
        return jdbc.sql(
                        """
                INSERT INTO course (slug, title, summary) VALUES (:slug, :title, :summary)
                ON CONFLICT (slug) DO NOTHING
                RETURNING id, slug, title, summary, published_at
                """)
                .param("slug", input.slug())
                .param("title", input.title().strip())
                .param("summary", input.summary().strip())
                .query(COURSE)
                .optional();
    }

    CourseAdminDtos.CourseRow updateCourse(long id, CourseAdminDtos.UpdateCourse input) {
        return jdbc.sql(
                        """
                UPDATE course SET title = :title, summary = :summary, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id RETURNING id, slug, title, summary, published_at
                """)
                .param("id", id)
                .param("title", input.title().strip())
                .param("summary", input.summary().strip())
                .query(COURSE)
                .single();
    }

    CourseAdminDtos.CourseRow setCoursePublication(long id, boolean published) {
        return jdbc.sql(
                        """
                UPDATE course
                SET published_at = CASE WHEN :published THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id RETURNING id, slug, title, summary, published_at
                """)
                .param("id", id)
                .param("published", published)
                .query(COURSE)
                .single();
    }

    List<CourseAdminDtos.LessonItem> listLessons(long courseId) {
        return jdbc.sql(
                        """
                SELECT slug, title, position, published_at FROM lesson
                WHERE course_id = :courseId ORDER BY position
                """)
                .param("courseId", courseId)
                .query(
                        (row, index) ->
                                new CourseAdminDtos.LessonItem(
                                        row.getString("slug"),
                                        row.getString("title"),
                                        row.getInt("position"),
                                        row.getObject("published_at", OffsetDateTime.class)))
                .list();
    }

    long countPublishedLessons(long courseId) {
        return jdbc.sql(
                        """
                SELECT count(*) FROM lesson WHERE course_id = :courseId AND published_at IS NOT NULL
                """)
                .param("courseId", courseId)
                .query(Long.class)
                .single();
    }

    Optional<CourseAdminDtos.LessonDetail> findLesson(long courseId, String slug) {
        return jdbc.sql(
                        """
                SELECT slug, title, position, body_markdown, published_at
                FROM lesson WHERE course_id = :courseId AND slug = :slug
                """)
                .param("courseId", courseId)
                .param("slug", slug)
                .query(LESSON)
                .optional();
    }

    Optional<CourseAdminDtos.LessonDetail> insertLesson(
            long courseId, CourseAdminDtos.CreateLesson input) {
        return jdbc.sql(
                        """
                INSERT INTO lesson (course_id, slug, title, position, body_markdown)
                SELECT :courseId, :slug, :title, COALESCE(MAX(position), 0) + 1, :body
                FROM lesson WHERE course_id = :courseId
                ON CONFLICT (course_id, slug) DO NOTHING
                RETURNING slug, title, position, body_markdown, published_at
                """)
                .param("courseId", courseId)
                .param("slug", input.slug())
                .param("title", input.title().strip())
                .param("body", input.bodyMarkdown())
                .query(LESSON)
                .optional();
    }

    CourseAdminDtos.LessonDetail updateLesson(
            long courseId, String slug, CourseAdminDtos.UpdateLesson input) {
        return jdbc.sql(
                        """
                UPDATE lesson SET title = :title, body_markdown = :body, updated_at = CURRENT_TIMESTAMP
                WHERE course_id = :courseId AND slug = :slug
                RETURNING slug, title, position, body_markdown, published_at
                """)
                .param("courseId", courseId)
                .param("slug", slug)
                .param("title", input.title().strip())
                .param("body", input.bodyMarkdown())
                .query(LESSON)
                .single();
    }

    CourseAdminDtos.LessonDetail setLessonPublication(
            long courseId, String slug, boolean published) {
        return jdbc.sql(
                        """
                UPDATE lesson
                SET published_at = CASE WHEN :published THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE course_id = :courseId AND slug = :slug
                RETURNING slug, title, position, body_markdown, published_at
                """)
                .param("courseId", courseId)
                .param("slug", slug)
                .param("published", published)
                .query(LESSON)
                .single();
    }

    void orderLessons(long courseId, List<String> slugs) {
        jdbc.sql(
                        """
                UPDATE lesson SET position = requested.position, updated_at = CURRENT_TIMESTAMP
                FROM unnest(CAST(:slugs AS text[])) WITH ORDINALITY AS requested(slug, position)
                WHERE lesson.course_id = :courseId AND lesson.slug = requested.slug
                  AND lesson.position <> requested.position
                """)
                .param("slugs", slugs.toArray(String[]::new))
                .param("courseId", courseId)
                .update();
    }
}

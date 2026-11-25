-- Check unique positions at the end of a reorder statement, including swaps.
ALTER TABLE lesson DROP CONSTRAINT lesson_course_id_position_key;
ALTER TABLE lesson ADD CONSTRAINT lesson_course_position_key
    UNIQUE (course_id, position) DEFERRABLE INITIALLY IMMEDIATE;

CREATE VIEW published_lesson AS
SELECT lesson.id, lesson.course_id, course.slug AS course_slug, lesson.slug,
       lesson.title, lesson.position, lesson.body_markdown
FROM lesson
JOIN course ON course.id = lesson.course_id
WHERE lesson.published_at IS NOT NULL AND course.published_at IS NOT NULL;

CREATE INDEX exercise_lesson_lookup_idx ON exercise (lesson_id, id);

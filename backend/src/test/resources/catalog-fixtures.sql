INSERT INTO course (slug, title, summary, published_at)
VALUES ('alpha-fixture', 'Published course', 'Test fixture', CURRENT_TIMESTAMP),
       ('draft-fixture', 'Draft course', 'Test fixture', NULL);

INSERT INTO lesson (course_id, slug, title, position, body_markdown, published_at)
SELECT id, 'published-child', 'Published child', 1, 'Test fixture', CURRENT_TIMESTAMP
FROM course WHERE slug = 'draft-fixture';

INSERT INTO lesson (course_id, slug, title, position, body_markdown)
SELECT id, 'draft-child', 'Draft child', 100, 'Test fixture'
FROM course WHERE slug = 'python-co-ban';

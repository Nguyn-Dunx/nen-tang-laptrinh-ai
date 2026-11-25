INSERT INTO account (email, password_hash, role)
VALUES ('sandbox-fixture@example.test', 'fixture-hash', 'LEARNER')
ON CONFLICT (email) DO NOTHING;

INSERT INTO exercise (
    lesson_id, title, language, difficulty, statement_md, starter_code, grading_type, max_score
)
SELECT lesson.id, fixture.title, 'python', 'beginner', 'Fixture exercise', '', 'OUTPUT_MATCH', 10
FROM (VALUES
    ('python-co-ban', 'xin-chao-python', 'sandbox-published-exercise'),
    ('python-co-ban', 'draft-child', 'sandbox-draft-lesson-exercise'),
    ('draft-fixture', 'published-child', 'sandbox-draft-course-exercise')
) AS fixture(course_slug, lesson_slug, title)
JOIN course ON course.slug = fixture.course_slug
JOIN lesson ON lesson.course_id = course.id AND lesson.slug = fixture.lesson_slug
WHERE NOT EXISTS (
    SELECT 1 FROM exercise existing
    WHERE existing.lesson_id = lesson.id AND existing.title = fixture.title
);

INSERT INTO exercise_reference_solution (exercise_id, language, source_code)
SELECT id, 'python', 'CANARY_REFERENCE_SOLUTION'
FROM exercise
WHERE title = 'sandbox-published-exercise'
  AND NOT EXISTS (
      SELECT 1 FROM exercise_reference_solution solution WHERE solution.exercise_id = exercise.id
  );

INSERT INTO test_case (exercise_id, position, stdin, expected_output, is_public, weight)
SELECT exercise.id, cases.position, cases.stdin, cases.expected_output, cases.is_public, cases.weight
FROM exercise
CROSS JOIN (VALUES
    (1, 'public input', 'public output', true, 1),
    (2, 'CANARY_HIDDEN_STDIN', 'CANARY_HIDDEN_EXPECTED_OUTPUT', false, 1)
) AS cases(position, stdin, expected_output, is_public, weight)
WHERE exercise.title = 'sandbox-published-exercise'
  AND NOT EXISTS (
      SELECT 1 FROM test_case existing
      WHERE existing.exercise_id = exercise.id AND existing.position = cases.position
  );

INSERT INTO submission (user_id, exercise_id, language, source_code, status)
SELECT account.id, exercise.id, 'python', 'print("fixture")', 'QUEUED'
FROM account
CROSS JOIN exercise
WHERE account.email = 'sandbox-fixture@example.test'
  AND exercise.title = 'sandbox-published-exercise'
  AND NOT EXISTS (
      SELECT 1 FROM submission existing
      WHERE existing.user_id = account.id AND existing.exercise_id = exercise.id
  );

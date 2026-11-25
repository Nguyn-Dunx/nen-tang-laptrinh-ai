INSERT INTO exercise (
    lesson_id, title, language, difficulty, statement_md, starter_code, grading_type, max_score
)
SELECT lesson.id,
       'Tính tổng hai số',
       'python',
       'beginner',
       'Đọc hai số nguyên từ stdin và in tổng của chúng.',
       '',
       'OUTPUT_MATCH',
       100
FROM lesson
JOIN course ON course.id = lesson.course_id
WHERE course.slug = 'python-co-ban' AND lesson.slug = 'bien-va-gia-tri'
  AND NOT EXISTS (
      SELECT 1 FROM exercise existing
      WHERE existing.lesson_id = lesson.id AND existing.title = 'Tính tổng hai số'
  );

INSERT INTO exercise_reference_solution (exercise_id, language, source_code)
SELECT id, 'python', 'a, b = map(int, input().split())
print(a + b)'
FROM exercise
WHERE title = 'Tính tổng hai số'
  AND NOT EXISTS (
      SELECT 1 FROM exercise_reference_solution solution WHERE solution.exercise_id = exercise.id
  );

INSERT INTO test_case (exercise_id, position, stdin, expected_output, is_public, weight)
SELECT exercise.id, cases.position, cases.stdin, cases.expected_output, cases.is_public, cases.weight
FROM exercise
CROSS JOIN (VALUES
    (1, '2 3', '5', true, 1),
    (2, '-4 9', '5', false, 1)
) AS cases(position, stdin, expected_output, is_public, weight)
WHERE exercise.title = 'Tính tổng hai số'
  AND NOT EXISTS (
      SELECT 1 FROM test_case existing
      WHERE existing.exercise_id = exercise.id AND existing.position = cases.position
  );

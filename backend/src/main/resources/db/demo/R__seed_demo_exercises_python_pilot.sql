WITH pilot (lesson_slug, title, statement_md) AS (
    VALUES
    ('dieu-kien', 'Qua ngưỡng điểm', 'Đọc một số nguyên từ 0 đến 10. In PASS nếu số đó từ 5 trở lên, ngược lại in RETRY.'),
    ('vong-lap', 'Tổng từ 1 đến n', 'Đọc một số nguyên n từ 0 đến 10000. In tổng các số từ 1 đến n. Với n bằng 0, in 0.')
)
INSERT INTO exercise (lesson_id, title, language, difficulty, statement_md, starter_code, grading_type, max_score)
SELECT lesson.id, pilot.title, 'python', 'beginner', pilot.statement_md,
       '# Viết chương trình Python tại đây', 'OUTPUT_MATCH', 100
FROM pilot
JOIN lesson ON lesson.slug = pilot.lesson_slug
JOIN course ON course.id = lesson.course_id AND course.slug = 'python-co-ban'
WHERE NOT EXISTS (
    SELECT 1 FROM exercise existing WHERE existing.lesson_id = lesson.id AND existing.title = pilot.title
);

WITH cases (lesson_slug, title, position, stdin, expected_output, is_public) AS (
    VALUES
    ('dieu-kien', 'Qua ngưỡng điểm', 1, '5', 'PASS', true),
    ('dieu-kien', 'Qua ngưỡng điểm', 2, '4', 'RETRY', true),
    ('dieu-kien', 'Qua ngưỡng điểm', 3, '10', 'PASS', false),
    ('vong-lap', 'Tổng từ 1 đến n', 1, '3', '6', true),
    ('vong-lap', 'Tổng từ 1 đến n', 2, '0', '0', true),
    ('vong-lap', 'Tổng từ 1 đến n', 3, '10000', '50005000', false)
)
INSERT INTO test_case (exercise_id, position, stdin, expected_output, is_public, weight)
SELECT exercise.id, cases.position, cases.stdin, cases.expected_output, cases.is_public, 1
FROM cases
JOIN exercise ON exercise.title = cases.title
JOIN lesson ON lesson.id = exercise.lesson_id AND lesson.slug = cases.lesson_slug
JOIN course ON course.id = lesson.course_id AND course.slug = 'python-co-ban'
ON CONFLICT (exercise_id, position) DO NOTHING;

WITH solutions (lesson_slug, title, code) AS (
    VALUES
    ('dieu-kien', 'Qua ngưỡng điểm', $py$score = int(input())
print("PASS" if score >= 5 else "RETRY")$py$),
    ('vong-lap', 'Tổng từ 1 đến n', $py$n = int(input())
print(sum(range(1, n + 1)))$py$)
)
INSERT INTO exercise_reference_solution (exercise_id, language, source_code)
SELECT exercise.id, 'python', solutions.code
FROM solutions
JOIN exercise ON exercise.title = solutions.title
JOIN lesson ON lesson.id = exercise.lesson_id AND lesson.slug = solutions.lesson_slug
JOIN course ON course.id = lesson.course_id AND course.slug = 'python-co-ban'
ON CONFLICT (exercise_id) DO NOTHING;

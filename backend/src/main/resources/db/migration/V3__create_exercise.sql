CREATE TABLE exercise (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lesson_id bigint NOT NULL REFERENCES lesson (id) ON DELETE RESTRICT,
    title text NOT NULL,
    language text NOT NULL,
    difficulty text NOT NULL,
    statement_md text NOT NULL,
    starter_code text NOT NULL,
    grading_type text NOT NULL CHECK (grading_type IN ('OUTPUT_MATCH', 'UNIT_TEST', 'DOM')),
    max_score integer NOT NULL CHECK (max_score > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exercise_reference_solution (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_id bigint NOT NULL UNIQUE REFERENCES exercise (id) ON DELETE CASCADE,
    language text NOT NULL,
    source_code text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_case (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_id bigint NOT NULL REFERENCES exercise (id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position > 0),
    stdin text NOT NULL,
    expected_output text NOT NULL,
    is_public boolean NOT NULL DEFAULT false,
    weight integer NOT NULL CHECK (weight > 0),
    UNIQUE (exercise_id, position)
);

CREATE VIEW test_case_public AS
SELECT id, exercise_id, position, stdin, expected_output, weight
FROM test_case
WHERE is_public = true;

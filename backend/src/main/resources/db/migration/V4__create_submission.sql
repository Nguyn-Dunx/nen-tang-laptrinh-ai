CREATE TABLE submission (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    exercise_id bigint NOT NULL REFERENCES exercise (id) ON DELETE RESTRICT,
    language text NOT NULL,
    source_code text NOT NULL,
    status text NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
    verdict text CHECK (
        verdict IS NULL OR verdict IN (
            'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT', 'MEMORY_LIMIT', 'RUNTIME_ERROR', 'COMPILE_ERROR'
        )
    ),
    score numeric(7, 2),
    time_ms integer,
    memory_kb integer,
    error_message text,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at timestamptz,
    CHECK ((status = 'DONE') = (verdict IS NOT NULL)),
    CHECK (score IS NULL OR score >= 0)
);

CREATE TABLE submission_test_result (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    submission_id bigint NOT NULL REFERENCES submission (id) ON DELETE CASCADE,
    test_case_id bigint NOT NULL REFERENCES test_case (id) ON DELETE RESTRICT,
    passed boolean NOT NULL,
    verdict text NOT NULL CHECK (
        verdict IN (
            'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT', 'MEMORY_LIMIT', 'RUNTIME_ERROR', 'COMPILE_ERROR'
        )
    ),
    time_ms integer,
    memory_kb integer,
    actual_output text,
    UNIQUE (submission_id, test_case_id),
    CHECK (passed = (verdict = 'ACCEPTED'))
);

CREATE INDEX idx_submission_user_exercise_created
    ON submission (user_id, exercise_id, created_at);
CREATE INDEX idx_submission_stuck
    ON submission (status_updated_at)
    WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX idx_submission_pending_user
    ON submission (user_id)
    WHERE status IN ('QUEUED', 'RUNNING');
CREATE INDEX idx_submission_test_result_submission
    ON submission_test_result (submission_id);

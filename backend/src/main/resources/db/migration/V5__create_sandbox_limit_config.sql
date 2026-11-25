CREATE TABLE sandbox_limit_config (
    key text PRIMARY KEY CHECK (
        key IN (
            'submit_rate_max',
            'submit_rate_window_seconds',
            'code_size_max_bytes',
            'time_limit_ms',
            'memory_limit_kb',
            'max_processes',
            'max_pending_per_user'
        )
    ),
    value_int bigint NOT NULL CHECK (value_int > 0),
    unit text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

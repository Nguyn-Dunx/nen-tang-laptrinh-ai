INSERT INTO sandbox_limit_config (key, value_int, unit)
VALUES
    ('submit_rate_max', 5, 'count'),
    ('submit_rate_window_seconds', 60, 'seconds'),
    ('code_size_max_bytes', 65536, 'bytes'),
    ('time_limit_ms', 2000, 'ms'),
    ('wall_time_limit_ms', 5000, 'ms'),
    ('cpu_extra_time_ms', 200, 'ms'),
    ('memory_limit_kb', 65536, 'kb'),
    ('max_processes', 20, 'count'),
    ('max_pending_per_user', 3, 'count'),
    ('max_file_size_kb', 1024, 'kb'),
    ('max_output_bytes', 65536, 'bytes'),
    ('max_attempts', 3, 'count'),
    ('stuck_timeout_seconds', 60, 'seconds'),
    ('retry_backoff_seconds', 1, 'seconds'),
    ('poll_timeout_seconds', 30, 'seconds'),
    ('recovery_batch_size', 100, 'count')
ON CONFLICT (key) DO UPDATE
SET value_int = EXCLUDED.value_int,
    unit = EXCLUDED.unit,
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE sandbox_limit_config
    DROP CONSTRAINT sandbox_limit_config_key_check;

ALTER TABLE sandbox_limit_config
    ADD CONSTRAINT sandbox_limit_config_key_check
        CHECK (
            key IN (
                'submit_rate_max',
                'submit_rate_window_seconds',
                'code_size_max_bytes',
                'time_limit_ms',
                'wall_time_limit_ms',
                'cpu_extra_time_ms',
                'memory_limit_kb',
                'max_processes',
                'max_pending_per_user',
                'max_file_size_kb',
                'max_output_bytes',
                'max_attempts',
                'stuck_timeout_seconds',
                'retry_backoff_seconds',
                'poll_timeout_seconds',
                'recovery_batch_size'
            )
        );

ALTER TABLE submission
    ADD COLUMN retry_after timestamptz;

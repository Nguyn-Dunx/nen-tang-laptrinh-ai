package vn.edu.codelearn.platform.submission;

import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class SubmissionRateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """,
                    Long.class);

    private final StringRedisTemplate redis;

    SubmissionRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    boolean allow(long userId, long maxRequests, long windowSeconds) {
        long window = Math.floorDiv(Instant.now().getEpochSecond(), windowSeconds);
        String key = "sandbox:rate:" + userId + ":" + window;
        Long count =
                redis.execute(INCREMENT_WITH_EXPIRY, List.of(key), Long.toString(windowSeconds));
        return count != null && count <= maxRequests;
    }
}

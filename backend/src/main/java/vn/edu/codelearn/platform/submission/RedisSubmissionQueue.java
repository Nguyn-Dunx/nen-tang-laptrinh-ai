package vn.edu.codelearn.platform.submission;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisSubmissionQueue implements SubmissionQueue {

    private static final String QUEUE_KEY = "sandbox:submissions";

    private final StringRedisTemplate redis;

    RedisSubmissionQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void enqueue(long submissionId) {
        redis.opsForList().leftPush(QUEUE_KEY, Long.toString(submissionId));
    }

    @Override
    public Long dequeue() {
        String value = redis.opsForList().rightPop(QUEUE_KEY);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public void checkConnection() {
        redis.execute(RedisConnection::ping);
    }
}

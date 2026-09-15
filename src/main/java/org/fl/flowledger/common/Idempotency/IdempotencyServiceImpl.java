package org.fl.flowledger.common.Idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public String get(String key) {
        return redisTemplate.opsForValue()
                .get("transfer:idempotency:" + key);
    }

    public boolean reserve(String key) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(
                        "transfer:idempotency:" + key,
                        "PROCESSING",
                        TTL
                )
        );
    }

    public void complete(String key, UUID transferId) {
        redisTemplate.opsForValue().set(
                "transfer:idempotency:" + key,
                transferId.toString(),
                TTL
        );
    }

    public void delete(String key) {
        redisTemplate.delete("transfer:idempotency:" + key);
    }
}
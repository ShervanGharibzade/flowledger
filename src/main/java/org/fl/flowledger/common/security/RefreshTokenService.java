package org.fl.flowledger.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final Duration REFRESH_TOKEN_TTL =
            Duration.ofDays(7);

    public String createRefreshToken(Long userId) {

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String key = "refresh:" + token;

        redisTemplate.opsForValue().set(
                key,
                userId.toString(),
                REFRESH_TOKEN_TTL
        );

        return token;
    }

    public Long getUserId(String refreshToken) {

        String userId = redisTemplate.opsForValue()
                .get("refresh:" + refreshToken);

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired refresh token"
            );
        }

        return Long.valueOf(userId);
    }

    public void revoke(String refreshToken) {
        redisTemplate.delete("refresh:" + refreshToken);
    }
}
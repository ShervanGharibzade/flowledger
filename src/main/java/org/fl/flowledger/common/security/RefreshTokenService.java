package org.fl.flowledger.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final Duration REFRESH_TOKEN_TTL =
            Duration.ofDays(7);

    private static String tokenKey(String token) {
        return "refresh:" + token;
    }

    private static String userTokensKey(Long userId) {
        return "refresh_tokens_by_user:" + userId;
    }

    public String createRefreshToken(Long userId) {

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        redisTemplate.opsForValue().set(
                tokenKey(token),
                userId.toString(),
                REFRESH_TOKEN_TTL
        );

        String userKey = userTokensKey(userId);
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, REFRESH_TOKEN_TTL);

        return token;
    }

    public Long getUserId(String refreshToken) {

        String userId = redisTemplate.opsForValue()
                .get(tokenKey(refreshToken));

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired refresh token"
            );
        }

        return Long.valueOf(userId);
    }

    public void revoke(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(tokenKey(refreshToken));

        redisTemplate.delete(tokenKey(refreshToken));

        if (userId != null) {
            redisTemplate.opsForSet().remove(
                    userTokensKey(Long.valueOf(userId)),
                    refreshToken
            );
        }
    }


    public String rotate(String oldRefreshToken, Long userId) {
        revoke(oldRefreshToken);
        return createRefreshToken(userId);
    }


    public void revokeAll(Long userId) {
        String userKey = userTokensKey(userId);

        Set<String> tokens = redisTemplate.opsForSet().members(userKey);

        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(tokenKey(token));
            }
        }

        redisTemplate.delete(userKey);
    }
}

package org.fl.flowledger.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void createRefreshToken_storesTokenAndTracksItAgainstTheUser() {
        String token = refreshTokenService.createRefreshToken(10L);

        assertNotNull(token);
        assertFalse(token.isBlank());

        verify(valueOperations).set(eq("refresh:" + token), eq("10"), eq(Duration.ofDays(7)));
        verify(setOperations).add("refresh_tokens_by_user:10", token);
        verify(redisTemplate).expire(eq("refresh_tokens_by_user:10"), eq(Duration.ofDays(7)));
    }

    @Test
    void getUserId_returnsUserId_whenTokenExists() {
        when(valueOperations.get("refresh:abc")).thenReturn("55");

        Long userId = refreshTokenService.getUserId("abc");

        assertEquals(55L, userId);
    }

    @Test
    void getUserId_throwsIllegalArgument_whenTokenMissingOrExpired() {
        when(valueOperations.get("refresh:missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.getUserId("missing"));
    }

    @Test
    void revoke_deletesTokenKeyAndRemovesFromUsersSet() {
        when(valueOperations.get("refresh:abc")).thenReturn("10");

        refreshTokenService.revoke("abc");

        verify(redisTemplate).delete("refresh:abc");
        verify(setOperations).remove("refresh_tokens_by_user:10", "abc");
    }

    @Test
    void revoke_doesNotTouchUsersSet_whenTokenAlreadyGone() {
        when(valueOperations.get("refresh:already-gone")).thenReturn(null);

        refreshTokenService.revoke("already-gone");

        verify(redisTemplate).delete("refresh:already-gone");
        verifyNoInteractions(setOperations);
    }

    @Test
    void rotate_revokesOldTokenThenIssuesANewOne() {
        when(valueOperations.get("refresh:old")).thenReturn("10");

        String newToken = refreshTokenService.rotate("old", 10L);

        assertNotNull(newToken);
        assertNotEquals("old", newToken);

        // old token must be gone
        verify(redisTemplate).delete("refresh:old");
        // and a brand-new one issued and tracked
        verify(valueOperations).set(eq("refresh:" + newToken), eq("10"), eq(Duration.ofDays(7)));
    }

    @Test
    void revokeAll_deletesEveryTrackedTokenAndTheUsersSetItself() {
        when(setOperations.members("refresh_tokens_by_user:10"))
                .thenReturn(Set.of("token-a", "token-b"));

        refreshTokenService.revokeAll(10L);

        verify(redisTemplate).delete("refresh:token-a");
        verify(redisTemplate).delete("refresh:token-b");
        verify(redisTemplate).delete("refresh_tokens_by_user:10");
    }

    @Test
    void revokeAll_stillDeletesTheSetKey_whenUserHasNoTrackedTokens() {
        when(setOperations.members("refresh_tokens_by_user:10")).thenReturn(null);

        refreshTokenService.revokeAll(10L);

        verify(redisTemplate).delete("refresh_tokens_by_user:10");
    }
}

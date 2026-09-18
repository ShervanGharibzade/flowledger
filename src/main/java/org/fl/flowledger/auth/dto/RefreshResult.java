package org.fl.flowledger.auth.dto;

/**
 * Result of a refresh-token exchange. The refresh token is rotated on every
 * use (old one revoked, new one issued) so the controller can set an updated
 * refresh_token cookie while only the access token is returned in the body.
 */
public record RefreshResult(
        String accessToken,
        String refreshToken
) {
}

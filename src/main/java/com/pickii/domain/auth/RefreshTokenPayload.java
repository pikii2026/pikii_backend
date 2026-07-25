package com.pickii.domain.auth;

import java.time.LocalDateTime;

/**
 * Redis Refresh Token(auth:refresh:{MemberId}:{DeviceId})에 저장되는 값 (REDIS_POLICY.md 4)
 *
 * <pre>
 * {
 *     "refreshToken":"eyJhbGciOiJIUzI1NiJ9...",
 *     "issuedAt":"2026-07-09T21:00:00",
 *     "deviceUUID":"device-uuid-1234",
 *     "lastUsedAt":"2026-07-09T21:00:00"
 * }
 * </pre>
 */
public record RefreshTokenPayload(
        String refreshToken,
        LocalDateTime issuedAt,
        String deviceUUID,
        LocalDateTime lastUsedAt
) {
}

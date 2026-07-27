package com.pickii.domain.auth;

/**
 * Redis Access Token Blacklist(auth:blacklist:{AccessToken})에 저장되는 값 (REDIS_POLICY.md 5)
 *
 * <pre>
 * {
 *     "memberId":15,
 *     "reason":"LOGOUT"
 * }
 * </pre>
 */
public record BlacklistPayload(
        Long memberId,
        String reason
) {
}
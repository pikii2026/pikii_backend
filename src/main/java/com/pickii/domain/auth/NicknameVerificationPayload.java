package com.pickii.domain.auth;

/**
 * Verification Token(auth:verify:{UUID})에 저장되는 닉네임 확인 값 (REDIS_POLICY.md 7)
 *
 * <pre>
 * {
 *     "verificationType":"NICKNAME",
 *     "nickname":"pickii"
 * }
 * </pre>
 */
public record NicknameVerificationPayload(
        VerificationType verificationType,
        String nickname
) {
}

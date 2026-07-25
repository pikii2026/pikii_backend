package com.pickii.domain.auth;

/**
 * Verification Token(auth:verify:{UUID})에 저장되는 이메일 인증 값 (REDIS_POLICY.md 7)
 *
 * <pre>
 * {
 *     "verificationType":"EMAIL",
 *     "purpose":"SIGNUP",
 *     "email":"example@email.com"
 * }
 * </pre>
 */
public record EmailVerificationPayload(
        VerificationType verificationType,
        VerificationPurpose purpose,
        String email
) {
}

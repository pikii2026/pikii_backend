package com.pickii.domain.auth.dto;

/**
 * API_SPEC 1-10 소셜 로그인 응답
 */
public record SocialLoginResponse(
        String accessToken,
        String refreshToken
) {
}
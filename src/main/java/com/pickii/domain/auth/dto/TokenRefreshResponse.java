package com.pickii.domain.auth.dto;

/**
 * API_SPEC 1-6 토큰 갱신(Silent Refresh) 응답
 */
public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
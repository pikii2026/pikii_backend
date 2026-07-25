package com.pickii.domain.auth.dto;

/**
 * API_SPEC 1-5 로그인 응답
 */
public record LoginResponse(
        Long memberId,
        String nickname,
        String accessToken,
        String refreshToken
) {
}

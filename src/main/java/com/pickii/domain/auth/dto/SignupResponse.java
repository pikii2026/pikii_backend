package com.pickii.domain.auth.dto;

/**
 * API_SPEC 1-4 회원가입 응답
 */
public record SignupResponse(
        Long memberId,
        String email,
        String nickname
) {
}

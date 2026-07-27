package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 1-12 비밀번호 변경(로그인 상태) 요청
 */
public record PasswordChangeRequest(
        @NotBlank(message = "인증 토큰이 필요합니다.")
        String emailVerificationToken,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {
}
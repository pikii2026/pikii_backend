package com.pickii.domain.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-9 회원 탈퇴 요청
 */
public record WithdrawRequest(
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotBlank(message = "인증 토큰이 필요합니다.")
        String emailVerificationToken,

        @NotNull(message = "약관 동의 정보가 필요합니다.")
        @Valid
        WithdrawAgreements agreements
) {
}
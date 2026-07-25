package com.pickii.domain.auth.dto;

import com.pickii.domain.auth.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-2 이메일 인증번호 확인 요청
 */
public record EmailVerifyRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증번호를 입력해주세요.")
        String code,

        @NotNull(message = "인증 목적(type)을 지정해주세요.")
        VerificationPurpose type
) {
}

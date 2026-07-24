package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-5 로그인 요청
 */
public record LoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotNull(message = "autoLogin 값을 지정해주세요.")
        Boolean autoLogin,

        @NotBlank(message = "deviceId가 필요합니다.")
        String deviceId
) {
}

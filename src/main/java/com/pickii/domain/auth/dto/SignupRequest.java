package com.pickii.domain.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 1-4 회원가입 요청
 */
public record SignupRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하로 입력해주세요.")
        String nickname,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,20}$",
                message = "비밀번호는 8자 이상 20자 이하이며, 영문 대/소문자와 숫자를 각 1자 이상 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm,

        @NotBlank(message = "이메일 인증 토큰이 필요합니다.")
        String emailVerificationToken,

        @NotBlank(message = "닉네임 인증 토큰이 필요합니다.")
        String nicknameVerificationToken,

        @NotNull(message = "약관 동의 정보가 필요합니다.")
        @Valid
        SignupTerms terms
) {
}

package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 1-10 소셜 로그인 요청
 */
public record SocialLoginRequest(
        @NotBlank(message = "socialAccessToken이 필요합니다.")
        String socialAccessToken,

        @NotNull(message = "autoLogin 값을 지정해주세요.")
        Boolean autoLogin,

        @NotBlank(message = "deviceId가 필요합니다.")
        String deviceId
) {
}
package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 1-6 토큰 갱신(Silent Refresh) 요청
 */
public record TokenRefreshRequest(
        @NotBlank(message = "deviceId가 필요합니다.")
        String deviceId,

        @NotBlank(message = "refreshToken이 필요합니다.")
        String refreshToken
) {
}

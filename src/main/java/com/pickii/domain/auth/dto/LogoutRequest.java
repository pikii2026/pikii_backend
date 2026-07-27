package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 1-7 로그아웃 요청
 */
public record LogoutRequest(
        @NotBlank(message = "deviceId가 필요합니다.")
        String deviceId
) {
}
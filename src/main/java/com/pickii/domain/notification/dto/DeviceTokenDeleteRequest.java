package com.pickii.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 9-9 디바이스 토큰 삭제 요청
 */
public record DeviceTokenDeleteRequest(
        @NotBlank(message = "fcmToken은 필수입니다.")
        String fcmToken
) {
}

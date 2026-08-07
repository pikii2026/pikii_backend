package com.pickii.domain.notification.dto;

import com.pickii.domain.notification.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 9-8 디바이스 토큰 등록 요청
 */
public record DeviceTokenRegisterRequest(
        @NotBlank(message = "fcmToken은 필수입니다.")
        String fcmToken,

        @NotNull(message = "platform은 필수입니다.")
        Platform platform
) {
}

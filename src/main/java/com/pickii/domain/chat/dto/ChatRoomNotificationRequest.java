package com.pickii.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 8-8 채팅방 알림 설정 요청
 */
public record ChatRoomNotificationRequest(
        @NotNull(message = "알림 수신 여부는 필수입니다.")
        Boolean enabled
) {
}

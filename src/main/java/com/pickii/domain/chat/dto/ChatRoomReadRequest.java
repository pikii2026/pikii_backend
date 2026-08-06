package com.pickii.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 8-6 채팅방 읽음 처리 요청
 */
public record ChatRoomReadRequest(
        @NotBlank(message = "마지막으로 읽은 메시지 ID는 필수입니다.")
        String lastReadMessageId
) {
}

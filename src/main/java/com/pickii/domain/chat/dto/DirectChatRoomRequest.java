package com.pickii.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 8-5 1:1 채팅방 생성 요청
 */
public record DirectChatRoomRequest(
        @NotNull(message = "대화 상대 회원 ID는 필수입니다.")
        Long targetMemberId
) {
}

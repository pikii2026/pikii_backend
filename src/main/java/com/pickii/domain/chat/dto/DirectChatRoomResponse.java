package com.pickii.domain.chat.dto;

import com.pickii.domain.chat.entity.ChatRoomType;

/**
 * API_SPEC 8-5 1:1 채팅방 생성 응답
 */
public record DirectChatRoomResponse(
        Long chatRoomId,
        ChatRoomType type,
        boolean isNew
) {
}

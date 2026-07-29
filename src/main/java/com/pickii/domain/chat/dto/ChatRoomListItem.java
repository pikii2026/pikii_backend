package com.pickii.domain.chat.dto;

import com.pickii.domain.chat.entity.ChatRoomType;

import java.time.OffsetDateTime;

/**
 * API_SPEC 8-1 채팅방 목록 항목
 */
public record ChatRoomListItem(
        Long chatRoomId,
        ChatRoomType type,
        String title,
        Long projectId,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        long unreadCount,
        boolean notiEnabled
) {
}

package com.pickii.domain.chat.dto;

import com.pickii.domain.chat.entity.ChatMessageType;

import java.time.OffsetDateTime;

/**
 * API_SPEC 8-3 이전 채팅 메시지 항목
 */
public record ChatMessageResponse(
        String messageId,
        Long senderId,
        String senderNickname,
        ChatMessageType type,
        String message,
        String imageUrl,
        OffsetDateTime createdAt
) {
}

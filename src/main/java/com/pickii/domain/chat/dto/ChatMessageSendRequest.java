package com.pickii.domain.chat.dto;

import com.pickii.domain.chat.entity.ChatMessageType;

/**
 * WebSocket 채팅 메시지 전송 요청 (STOMP /pub/chatrooms/{chatRoomId}/messages)
 * senderId는 클라이언트 입력을 신뢰하지 않고 STOMP 세션의 인증된 Principal에서만 가져온다.
 */
public record ChatMessageSendRequest(
        ChatMessageType type,
        String message,
        String imageUrl
) {
}

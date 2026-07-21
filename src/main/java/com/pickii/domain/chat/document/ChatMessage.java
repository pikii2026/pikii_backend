package com.pickii.domain.chat.document;

import com.pickii.domain.chat.entity.ChatMessageType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 (MongoDB Collection).
 * 이미지는 Object Storage에 업로드 후 접근 URL만 저장한다.
 * 작성자 탈퇴 시 memberId는 null 처리하고 '알 수 없음'으로 표시한다.
 */
@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private Long chatRoomId;

    private Long memberId;

    private ChatMessageType type;

    /** 텍스트 본문 (IMAGE 타입이면 null) */
    private String message;

    /** 이미지 접근 URL (TEXT 타입이면 null) */
    private String imageUrl;

    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(Long chatRoomId, Long memberId, ChatMessageType type,
                       String message, String imageUrl) {
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.type = type;
        this.message = message;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }
}

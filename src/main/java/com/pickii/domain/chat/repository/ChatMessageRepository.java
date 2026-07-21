package com.pickii.domain.chat.repository;

import com.pickii.domain.chat.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findAllByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    /** 읽음 커서 이후 메시지 수 (unreadCount 계산용) */
    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, String lastReadMessageId);
}

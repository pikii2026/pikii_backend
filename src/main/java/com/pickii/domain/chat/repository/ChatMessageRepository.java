package com.pickii.domain.chat.repository;

import com.pickii.domain.chat.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findAllByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    /** 8-3: cursor 이전(더 오래된) 메시지 조회 */
    Page<ChatMessage> findAllByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(Long chatRoomId, String id, Pageable pageable);

    /** 읽음 커서 이후 메시지 수 (unreadCount 계산용) */
    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, String lastReadMessageId);

    /** 읽음 커서가 없을 때(한 번도 읽지 않음) 전체 메시지 수 */
    long countByChatRoomId(Long chatRoomId);
}

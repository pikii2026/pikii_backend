package com.pickii.domain.chat.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 참여자. 읽음 여부는 메시지마다 저장하지 않고 사용자별 읽음 커서 1개만 저장한다.
 * unreadCount = lastReadMessageId 이후에 생성된 메시지 수.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    /** 마지막으로 읽은 메시지의 MongoDB ObjectId */
    private String lastReadMessageId;

    private LocalDateTime lastReadAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /** 채팅방별 알림 on/off. 실제 발송 조건 = 전역 ChatNoti AND notiEnabled */
    @Column(nullable = false)
    private boolean notiEnabled;

    @Builder
    public ChatRoomMember(ChatRoom chatRoom, Member member) {
        this.chatRoom = chatRoom;
        this.member = member;
        this.joinedAt = LocalDateTime.now();
        this.notiEnabled = true;
    }

    public void updateReadCursor(String messageId) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = LocalDateTime.now();
    }

    public void toggleNoti(boolean enabled) {
        this.notiEnabled = enabled;
    }
}

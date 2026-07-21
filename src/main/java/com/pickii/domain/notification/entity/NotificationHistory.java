package com.pickii.domain.notification.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 수신 내역. referenceType/referenceId로 클릭 시 딥링크 이동한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationReferenceType referenceType;

    private Long referenceId;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @Builder
    public NotificationHistory(Member member, String title, String content, NotificationType type,
                               NotificationReferenceType referenceType, Long referenceId) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.isRead = false;
        this.sentAt = LocalDateTime.now();
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}

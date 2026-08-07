package com.pickii.domain.notification.event;

import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationType;

/**
 * NotificationHistory가 저장될 때마다 발행되는 이벤트. 트랜잭션 커밋 이후 시스템 푸시(FCM) 발송에 사용한다.
 */
public record NotificationCreatedEvent(
        Long memberId,
        String title,
        String content,
        NotificationType type,
        NotificationReferenceType referenceType,
        Long referenceId
) {
}

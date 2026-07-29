package com.pickii.domain.notification.dto;

import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationType;

import java.time.OffsetDateTime;

/**
 * API_SPEC 9-1 알림 목록 항목
 */
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        NotificationReferenceType referenceType,
        Long referenceId,
        boolean isRead,
        OffsetDateTime sentAt,
        OffsetDateTime readAt
) {
}

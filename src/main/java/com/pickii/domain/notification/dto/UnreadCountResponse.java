package com.pickii.domain.notification.dto;

/**
 * API_SPEC 9-7 안 읽은 알림 개수 조회 응답
 */
public record UnreadCountResponse(
        long unreadCount
) {
}

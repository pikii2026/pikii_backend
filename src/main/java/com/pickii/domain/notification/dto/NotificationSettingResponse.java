package com.pickii.domain.notification.dto;

/**
 * API_SPEC 9-5 알림 설정 조회 응답
 */
public record NotificationSettingResponse(
        boolean chatNoti,
        boolean applicantNoti,
        boolean commentNoti,
        boolean scheduleNoti,
        boolean matchNoti,
        boolean projectNoti,
        boolean marketingNoti
) {
}

package com.pickii.domain.notification.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 9-6 알림 설정 수정 요청 (조회 응답과 동일한 필드 구조)
 */
public record NotificationSettingRequest(
        @NotNull(message = "채팅 알림 설정값은 필수입니다.")
        Boolean chatNoti,
        @NotNull(message = "새 지원자 알림 설정값은 필수입니다.")
        Boolean applicantNoti,
        @NotNull(message = "댓글 알림 설정값은 필수입니다.")
        Boolean commentNoti,
        @NotNull(message = "일정 알림 설정값은 필수입니다.")
        Boolean scheduleNoti,
        @NotNull(message = "매칭 결과 알림 설정값은 필수입니다.")
        Boolean matchNoti,
        @NotNull(message = "프로젝트 알림 설정값은 필수입니다.")
        Boolean projectNoti,
        @NotNull(message = "마케팅 수신 동의 여부는 필수입니다.")
        Boolean marketingNoti
) {
}

package com.pickii.domain.resume.dto;

/**
 * API_SPEC 4-1 내 프로필 조회 응답 - 수상 및 경험 (날짜는 "YYYY-MM" 포맷, endDate는 진행 중이면 null)
 */
public record ExperienceItem(
        String startDate,
        String endDate,
        String title,
        String organization,
        String description
) {
}

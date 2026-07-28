package com.pickii.domain.resume.dto;

/**
 * API_SPEC 4-1 내 프로필 조회 응답 - 자격증 (date는 "YYYY-MM" 포맷)
 */
public record LicenseItem(
        String licenseName,
        String date
) {
}

package com.pickii.domain.resume.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 4-2/4-3 프로필 생성/수정 요청 - 자격증 (date는 "YYYY-MM" 포맷)
 */
public record LicenseRequest(
        @NotBlank(message = "자격증명을 입력해주세요.")
        String licenseName,

        @NotBlank(message = "취득일자를 입력해주세요.")
        String date
) {
}

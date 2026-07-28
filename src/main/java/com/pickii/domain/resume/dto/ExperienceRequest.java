package com.pickii.domain.resume.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 4-2/4-3 프로필 생성/수정 요청 - 수상 및 경험
 * (날짜는 "YYYY-MM" 포맷, endDate는 진행 중이면 생략 가능)
 */
public record ExperienceRequest(
        @NotBlank(message = "시작일을 입력해주세요.")
        String startDate,

        String endDate,

        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        String organization,

        String description
) {
}

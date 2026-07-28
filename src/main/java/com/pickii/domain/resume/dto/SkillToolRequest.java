package com.pickii.domain.resume.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 4-2/4-3 프로필 생성/수정 요청 - 기술 스택/툴
 */
public record SkillToolRequest(
        @NotBlank(message = "기술 스택명을 입력해주세요.")
        String techStackName,

        @NotNull(message = "숙련도를 입력해주세요.")
        @Min(value = 1, message = "숙련도는 1~3 사이여야 합니다.")
        @Max(value = 3, message = "숙련도는 1~3 사이여야 합니다.")
        Integer level
) {
}

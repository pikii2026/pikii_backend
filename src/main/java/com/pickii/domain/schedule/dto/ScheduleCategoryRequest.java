package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 7-2 일정 카테고리 생성 / 7-3 일정 카테고리 수정 요청
 */
public record ScheduleCategoryRequest(
        @NotBlank(message = "카테고리명을 입력해주세요.")
        @Size(max = 15, message = "카테고리명은 15자 이하여야 합니다.")
        String title,

        @NotBlank(message = "색상을 입력해주세요.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식의 HEX 코드여야 합니다.")
        String color
) {
}

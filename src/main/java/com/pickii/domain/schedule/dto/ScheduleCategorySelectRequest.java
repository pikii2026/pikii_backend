package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

/**
 * API_SPEC 7-19 프로젝트 색상(카테고리) 지정 - 개인별 요청
 */
public record ScheduleCategorySelectRequest(
        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId
) {
}

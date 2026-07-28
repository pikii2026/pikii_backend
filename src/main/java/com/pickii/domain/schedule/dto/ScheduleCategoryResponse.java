package com.pickii.domain.schedule.dto;

import com.pickii.domain.schedule.entity.ScheduleCategory;

/**
 * API_SPEC 7-1 일정 카테고리 목록 조회 응답 항목 (7-5 일정 조회의 category 필드로도 재사용)
 */
public record ScheduleCategoryResponse(
        Long categoryId,
        String title,
        String color
) {
    public static ScheduleCategoryResponse from(ScheduleCategory category) {
        return new ScheduleCategoryResponse(category.getId(), category.getTitle(), category.getColor());
    }
}

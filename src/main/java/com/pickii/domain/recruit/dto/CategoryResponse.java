package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.Category;

/**
 * API_SPEC 5-1 카테고리 조회 응답 항목
 */
public record CategoryResponse(
        Long categoryId,
        String name
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}

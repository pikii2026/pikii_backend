package com.pickii.domain.apply.dto;

import com.pickii.domain.apply.entity.ApplyKeyword;

import java.util.List;

/**
 * API_SPEC 5-7 지원 키워드 조회 응답 (카테고리 안에 키워드가 중첩된 Nested 구조)
 */
public record ApplyKeywordCategoryResponse(
        Long categoryId,
        String category,
        List<KeywordItem> keywords
) {
    public record KeywordItem(Long keywordId, String content) {
        public static KeywordItem from(ApplyKeyword keyword) {
            return new KeywordItem(keyword.getId(), keyword.getContent());
        }
    }
}

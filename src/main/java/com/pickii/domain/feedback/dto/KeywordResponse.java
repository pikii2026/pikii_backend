package com.pickii.domain.feedback.dto;

import com.pickii.domain.feedback.entity.Keyword;

/**
 * API_SPEC 5-6 피드백 키워드 조회 응답 항목
 */
public record KeywordResponse(
        Long keywordId,
        String name
) {
    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(keyword.getId(), keyword.getName());
    }
}

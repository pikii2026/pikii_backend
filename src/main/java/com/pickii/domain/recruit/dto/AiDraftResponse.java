package com.pickii.domain.recruit.dto;

/**
 * API_SPEC 3-3 AI 공고 초안 생성 응답
 */
public record AiDraftResponse(
        String simpleDesc,
        String content
) {
}

package com.pickii.domain.feedback.dto;

import java.util.List;

/**
 * API_SPEC 4-12 AI 피드백 상세 조회 응답
 */
public record AiFeedbackDetailResponse(
        Long projectId,
        List<String> keywords,
        String strengthSummary,
        String weaknessSummary
) {
}

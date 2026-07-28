package com.pickii.domain.feedback.dto;

/**
 * API_SPEC 4-11 AI 피드백 목록 조회 응답 항목
 */
public record FeedbackListResponse(
        Long projectId,
        String name,
        EvaluationPeriodResponse evaluationPeriod,
        int remainingDays,
        int memberCount,
        int evaluatedCount,
        int requiredCount,
        boolean isAiFeedbackAvailable
) {
}

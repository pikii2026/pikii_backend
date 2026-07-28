package com.pickii.domain.feedback.dto;

import java.time.LocalDate;

/**
 * API_SPEC 4-11 평가 기간 (프로젝트 종료일 ~ +3일)
 */
public record EvaluationPeriodResponse(
        LocalDate start,
        LocalDate end
) {
}

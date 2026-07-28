package com.pickii.domain.feedback.dto;

/**
 * API_SPEC 4-9 평가 대상 팀원 항목
 */
public record TargetMemberResponse(
        Long memberId,
        String nickname,
        boolean isEvaluated
) {
}

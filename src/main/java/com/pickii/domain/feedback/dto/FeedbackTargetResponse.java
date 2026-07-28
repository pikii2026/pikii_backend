package com.pickii.domain.feedback.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API_SPEC 4-9 평가 대상 팀원 조회 응답
 */
public record FeedbackTargetResponse(
        Long projectId,
        OffsetDateTime evaluationDeadline,
        List<TargetMemberResponse> members
) {
}

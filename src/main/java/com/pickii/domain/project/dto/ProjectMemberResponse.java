package com.pickii.domain.project.dto;

import java.time.OffsetDateTime;

/**
 * API_SPEC 6-3 프로젝트 팀원 조회 응답 항목
 */
public record ProjectMemberResponse(
        Long memberId,
        String nickname,
        boolean isLeader,
        OffsetDateTime joinedAt
) {
}

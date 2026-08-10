package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.RecruitStatus;

import java.time.OffsetDateTime;

/**
 * API_SPEC 2-1 공고 검색/목록 조회 응답 항목
 */
public record RecruitSummaryResponse(
        Long recruitId,
        String title,
        Long authorId,
        String authorNickname,
        int maxMembers,
        int availableSlots,
        RecruitStatus status,
        OffsetDateTime createdAt
) {
}

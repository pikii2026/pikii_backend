package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.RecruitStatus;

import java.time.OffsetDateTime;

/**
 * API_SPEC 3-16 스크랩한 공고 목록 조회 응답
 */
public record RecruitScrapSummaryResponse(
        Long recruitId,
        String title,
        Long authorId,
        String authorNickname,
        int authorEXP,
        boolean onCampus,
        RecruitStatus status,
        int maxMembers,
        int availableSlots,
        OffsetDateTime scrappedAt
) {
}

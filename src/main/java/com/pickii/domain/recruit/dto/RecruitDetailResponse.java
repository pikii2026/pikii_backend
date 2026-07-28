package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.RecruitStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API_SPEC 3-1 공고 상세 조회 응답
 */
public record RecruitDetailResponse(
        Long recruitId,
        String title,
        Long authorId,
        String authorNickname,
        int authorEXP,
        OffsetDateTime createdAt,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> category,
        List<Long> topics,
        boolean onCampus,
        String simpleDesc,
        String content,
        RecruitStatus status,
        int maxMembers,
        int availableSlots,
        boolean isScrapped
) {
}

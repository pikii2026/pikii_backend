package com.pickii.domain.recruit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    /** 탈퇴한 작성자는 authorId가 null이다. 전역 non_null 직렬화 설정과 무관하게 항상 키를 내려준다. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Override
    public Long authorId() {
        return authorId;
    }
}

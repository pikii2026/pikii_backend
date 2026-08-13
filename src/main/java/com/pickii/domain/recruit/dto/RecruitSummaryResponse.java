package com.pickii.domain.recruit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        int authorEXP,
        int maxMembers,
        int availableSlots,
        RecruitStatus status,
        OffsetDateTime createdAt
) {
    /** 탈퇴한 작성자는 authorId가 null이다. 전역 non_null 직렬화 설정과 무관하게 항상 키를 내려준다. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Override
    public Long authorId() {
        return authorId;
    }
}

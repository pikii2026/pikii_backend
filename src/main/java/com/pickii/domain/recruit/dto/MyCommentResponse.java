package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.RecruitStatus;

import java.time.OffsetDateTime;

/**
 * API_SPEC 4-6 작성한 댓글 조회 응답 항목
 */
public record MyCommentResponse(
        Long commentId,
        Long recruitId,
        String recruitTitle,
        RecruitStatus recruitStatus,
        String content,
        OffsetDateTime createdAt
) {
}

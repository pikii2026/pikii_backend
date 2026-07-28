package com.pickii.domain.apply.dto;

import com.pickii.domain.apply.entity.ApplyStatus;
import com.pickii.domain.recruit.entity.RecruitStatus;

import java.time.OffsetDateTime;

/**
 * API_SPEC 4-4 지원 현황 조회 응답 항목
 */
public record MyApplyResponse(
        Long applyId,
        Long recruitId,
        String recruitTitle,
        RecruitStatus recruitStatus,
        ApplyStatus status,
        OffsetDateTime createdAt
) {
}

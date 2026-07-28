package com.pickii.domain.apply.dto;

import com.pickii.domain.apply.entity.ApplyStatus;

import java.time.OffsetDateTime;

/**
 * API_SPEC 4-7 지원자 목록 조회 응답 항목
 */
public record ApplicantResponse(
        Long applyId,
        Long memberId,
        String nickname,
        String message,
        ApplyStatus status,
        OffsetDateTime createdAt
) {
}

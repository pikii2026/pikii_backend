package com.pickii.domain.member.dto;

import com.pickii.domain.member.entity.Univ;

/**
 * API_SPEC 5-8 대학교 목록 조회 응답 항목
 */
public record UnivResponse(
        Long univId,
        String name
) {
    public static UnivResponse from(Univ univ) {
        return new UnivResponse(univ.getId(), univ.getName());
    }
}

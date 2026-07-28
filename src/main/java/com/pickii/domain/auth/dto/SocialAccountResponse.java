package com.pickii.domain.auth.dto;

import com.pickii.domain.member.entity.LoginProvider;

import java.time.OffsetDateTime;

/**
 * API_SPEC 1-13 소셜 계정 연동 상태 조회 응답
 */
public record SocialAccountResponse(
        LoginProvider provider,
        boolean linked,
        OffsetDateTime linkedAt
) {
}
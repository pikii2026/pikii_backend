package com.pickii.domain.resume.dto;

/**
 * API_SPEC 4-1 내 프로필 조회 응답 - 외부 포트폴리오 링크
 */
public record AdditionalLinkItem(
        String linkName,
        String url
) {
}

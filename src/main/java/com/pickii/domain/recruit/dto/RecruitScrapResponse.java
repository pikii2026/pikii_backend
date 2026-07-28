package com.pickii.domain.recruit.dto;

/**
 * API_SPEC 3-14 공고 스크랩 응답
 */
public record RecruitScrapResponse(
        Long recruitId,
        boolean isScrapped
) {
}

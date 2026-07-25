package com.pickii.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API_SPEC 1-3 닉네임 중복 확인 응답
 */
public record NicknameCheckResponse(
        @JsonProperty("isAvailable") boolean available,
        String nicknameVerificationToken
) {
}

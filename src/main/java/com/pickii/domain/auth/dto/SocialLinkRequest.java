package com.pickii.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 1-11 소셜 계정 연동 요청
 */
public record SocialLinkRequest(
        @NotBlank(message = "providerId가 필요합니다.")
        String providerId
) {
}
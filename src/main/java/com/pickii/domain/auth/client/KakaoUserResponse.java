package com.pickii.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 카카오 GET /v2/user/me 응답 중 필요한 필드만 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(
        Long id
) {
}
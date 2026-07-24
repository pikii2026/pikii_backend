package com.pickii.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API_SPEC 1-2 이메일 인증번호 확인 응답
 *
 * <p>record 컴포넌트명을 그대로 두면 boolean getter 이름 규칙 때문에
 * JSON 키가 "isVerified"가 아니라 "verified"로 나가버릴 수 있어 {@link JsonProperty}로 고정한다.</p>
 */
public record EmailVerifyResponse(
        @JsonProperty("isVerified") boolean verified,
        String emailVerificationToken
) {
}

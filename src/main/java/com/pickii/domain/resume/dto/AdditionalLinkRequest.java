package com.pickii.domain.resume.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 4-2/4-3 프로필 생성/수정 요청 - 외부 포트폴리오 링크
 */
public record AdditionalLinkRequest(
        @NotBlank(message = "링크 카테고리명을 입력해주세요.")
        String linkName,

        @NotBlank(message = "URL을 입력해주세요.")
        String url
) {
}

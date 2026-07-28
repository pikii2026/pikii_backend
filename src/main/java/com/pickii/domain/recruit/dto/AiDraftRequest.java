package com.pickii.domain.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 3-3 AI 공고 초안 생성 요청
 */
public record AiDraftRequest(
        @Size(max = 50, message = "간단 소개는 50자 이하로 입력해주세요.")
        String simpleDesc,

        @NotBlank(message = "상세 내용을 입력해주세요.")
        @Size(max = 1000, message = "상세 내용은 1000자 이하로 입력해주세요.")
        String content
) {
}

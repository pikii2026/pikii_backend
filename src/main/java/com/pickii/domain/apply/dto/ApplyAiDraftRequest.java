package com.pickii.domain.apply.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API_SPEC 3-10 AI 지원서 초안 생성 요청
 */
public record ApplyAiDraftRequest(
        @NotBlank(message = "원본 메시지를 입력해주세요.")
        String message
) {
}

package com.pickii.domain.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * API_SPEC 3-11 공고 지원하기 요청
 */
public record ApplyCreateRequest(
        @NotBlank(message = "지원 메시지를 입력해주세요.")
        @Size(max = 300, message = "지원 메시지는 300자 이하여야 합니다.")
        String message,

        @Size(max = 5, message = "키워드는 최대 5개까지 선택할 수 있습니다.")
        List<Long> keywordIds
) {
}

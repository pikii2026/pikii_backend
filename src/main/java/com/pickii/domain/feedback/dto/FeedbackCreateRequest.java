package com.pickii.domain.feedback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 4-10 상호평가 작성 요청
 */
public record FeedbackCreateRequest(
        @NotNull(message = "프로젝트를 입력해주세요.")
        Long projectId,

        @NotNull(message = "평가 대상 팀원을 입력해주세요.")
        Long revieweeId,

        @NotNull(message = "평가 점수를 입력해주세요.")
        @Valid
        ScoresRequest scores,

        @NotBlank(message = "장점을 입력해주세요.")
        @Size(min = 30, max = 500, message = "장점은 30~500자여야 합니다.")
        String strength,

        @NotBlank(message = "개선점을 입력해주세요.")
        @Size(min = 30, max = 500, message = "개선점은 30~500자여야 합니다.")
        String weakness
) {
}

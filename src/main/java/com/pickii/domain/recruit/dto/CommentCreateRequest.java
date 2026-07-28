package com.pickii.domain.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * API_SPEC 3-8 댓글 및 답글 작성 요청
 */
public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(min = 2, max = 100, message = "댓글은 2~100자여야 합니다.")
        String content,

        Long parentCommentId
) {
}

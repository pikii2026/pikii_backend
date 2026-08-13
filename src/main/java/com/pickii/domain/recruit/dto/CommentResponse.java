package com.pickii.domain.recruit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API_SPEC 3-7 댓글 및 답글 목록 조회 응답 - 부모 댓글
 */
public record CommentResponse(
        Long commentId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        boolean isAuthor,
        List<CommentReplyResponse> replies
) {
    /** 탈퇴한 작성자는 authorId가 null이다. 전역 non_null 직렬화 설정과 무관하게 항상 키를 내려준다. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Override
    public Long authorId() {
        return authorId;
    }
}

package com.pickii.domain.recruit.dto;

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
}

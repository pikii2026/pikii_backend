package com.pickii.domain.recruit.dto;

import java.time.OffsetDateTime;

/**
 * API_SPEC 3-7 댓글 및 답글 목록 조회 응답 - 답글
 */
public record CommentReplyResponse(
        Long commentId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        boolean isAuthor
) {
}

package com.pickii.domain.recruit.dto;

import java.util.List;

/**
 * API_SPEC 3-7 댓글 및 답글 목록 조회 응답
 */
public record CommentListResponse(
        List<CommentResponse> comments
) {
}

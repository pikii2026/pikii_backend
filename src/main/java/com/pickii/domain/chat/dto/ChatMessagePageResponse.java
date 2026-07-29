package com.pickii.domain.chat.dto;

import java.util.List;

/**
 * API_SPEC 8-3 응답 (cursor 기반 페이지네이션)
 */
public record ChatMessagePageResponse(
        List<ChatMessageResponse> content,
        String nextCursor,
        boolean hasNext
) {
}

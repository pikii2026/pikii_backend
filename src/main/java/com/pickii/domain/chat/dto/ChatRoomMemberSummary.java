package com.pickii.domain.chat.dto;

/**
 * API_SPEC 8-2 채팅방 참여자 요약
 */
public record ChatRoomMemberSummary(
        Long memberId,
        String nickname
) {
}

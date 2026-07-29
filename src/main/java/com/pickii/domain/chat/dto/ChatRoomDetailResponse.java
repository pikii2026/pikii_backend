package com.pickii.domain.chat.dto;

import com.pickii.domain.project.entity.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * API_SPEC 8-2 채팅방 상세 조회 응답.
 * GROUP 채팅방이면 projectId/startDate/endDate/status/isLeader를 함께 채운다(DIRECT는 null → 응답에서 제외).
 */
public record ChatRoomDetailResponse(
        Long chatRoomId,
        String title,
        List<ChatRoomMemberSummary> members,
        Long projectId,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        Boolean isLeader
) {
}

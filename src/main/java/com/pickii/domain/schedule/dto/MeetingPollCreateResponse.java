package com.pickii.domain.schedule.dto;

import java.time.OffsetDateTime;

/**
 * API_SPEC 7-10 회의 조율 개설 응답
 */
public record MeetingPollCreateResponse(
        Long pollId,
        String status,
        OffsetDateTime deadline,
        int totalMembers,
        int respondedCount,
        int slotCount
) {
}

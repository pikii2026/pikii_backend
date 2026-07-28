package com.pickii.domain.schedule.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API_SPEC 7-11 응답 화면 조회 (슬롯 + 캘린더 프리필)
 */
public record MeetingPollDetailResponse(
        Long pollId,
        String title,
        String status,
        int durationMin,
        OffsetDateTime deadline,
        long totalMembers,
        long respondedCount,
        boolean myResponded,
        List<SlotItem> slots
) {
    public record SlotItem(
            Long slotId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            boolean myAvailable,
            boolean prefilledByCalendar,
            long availableCount,
            long unansweredCount
    ) {
    }
}

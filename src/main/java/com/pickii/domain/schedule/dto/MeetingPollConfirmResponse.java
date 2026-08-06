package com.pickii.domain.schedule.dto;

/**
 * API_SPEC 7-13 최종 일정 확정 응답
 */
public record MeetingPollConfirmResponse(
        Long pollId,
        String status,
        Long scheduleId
) {
}

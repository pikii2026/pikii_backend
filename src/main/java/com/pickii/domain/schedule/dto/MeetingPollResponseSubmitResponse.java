package com.pickii.domain.schedule.dto;

/**
 * API_SPEC 7-12 응답 제출 응답
 */
public record MeetingPollResponseSubmitResponse(
        Long pollId,
        long respondedCount,
        long totalMembers
) {
}

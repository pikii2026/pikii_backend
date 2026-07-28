package com.pickii.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-5 월별 일정 조회 응답 항목.
 * 단발 일정은 date만, 반복 일정은 startDate/endDate/rrule만 채워진다(반대쪽은 null로 생략).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyScheduleResponse(
        Long scheduleId,
        String title,
        boolean isRecurring,
        LocalDate date,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String rrule,
        String content,
        ScheduleCategoryResponse category
) {
}

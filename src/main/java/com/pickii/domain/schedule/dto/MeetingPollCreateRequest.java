package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * API_SPEC 7-10 회의 조율 개설 요청
 */
public record MeetingPollCreateRequest(
        @NotBlank(message = "회의명을 입력해주세요.")
        @Size(max = 20, message = "회의명은 20자 이하로 입력해주세요.")
        String title,

        @NotNull(message = "소요 시간(분)을 입력해주세요.")
        Integer durationMin,

        @NotNull(message = "탐색 시작일을 입력해주세요.")
        LocalDate rangeStart,

        @NotNull(message = "탐색 종료일을 입력해주세요.")
        LocalDate rangeEnd,

        @NotNull(message = "탐색 시작 시각을 입력해주세요.")
        LocalTime dayStart,

        @NotNull(message = "탐색 종료 시각을 입력해주세요.")
        LocalTime dayEnd,

        Integer deadlineHours,

        List<Long> memberIds
) {
}

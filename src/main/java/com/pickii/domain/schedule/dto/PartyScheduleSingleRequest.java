package com.pickii.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API_SPEC 7-16 팀 일정 직접 등록(단발) 요청.
 * 개인 단발 일정 생성(7-6)과 동일하되 categoryId는 없다 (색상은 7-19로 팀원별 지정).
 */
public record PartyScheduleSingleRequest(
        @NotBlank(message = "일정 제목을 입력해주세요.")
        String title,

        @NotNull(message = "일정 날짜를 입력해주세요.")
        LocalDate date,

        @NotNull(message = "시작 시간을 입력해주세요.")
        LocalTime startTime,

        @NotNull(message = "종료 시간을 입력해주세요.")
        LocalTime endTime,

        String content
) {
}

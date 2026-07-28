package com.pickii.domain.schedule.controller;

import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.service.MemberScheduleService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 개인 일정 조회 API (API_SPEC 7-5)
 */
@RestController
@RequestMapping("/users/me/schedules")
@RequiredArgsConstructor
public class MemberScheduleController {

    private final MemberScheduleService memberScheduleService;

    /** 7-5 월별 일정 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MyScheduleResponse>>> getMonthlySchedules(
            @AuthenticationPrincipal Long memberId,
            @RequestParam int year,
            @RequestParam int month) {
        List<MyScheduleResponse> response = memberScheduleService.getMonthlySchedules(memberId, year, month);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

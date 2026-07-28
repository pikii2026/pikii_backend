package com.pickii.domain.schedule.controller;

import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.dto.RecurringScheduleRequest;
import com.pickii.domain.schedule.dto.ScheduleCreateResponse;
import com.pickii.domain.schedule.dto.ScheduleUpdateRequest;
import com.pickii.domain.schedule.dto.SingleScheduleRequest;
import com.pickii.domain.schedule.service.MemberScheduleService;
import com.pickii.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 개인 일정 API (API_SPEC 7-5 ~ 7-9)
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

    /** 7-6 개인 단발 일정 생성 */
    @PostMapping("/single")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createSingle(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SingleScheduleRequest request) {
        ScheduleCreateResponse response = memberScheduleService.createSingle(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-7 개인 반복 일정 생성 */
    @PostMapping("/recurring")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createRecurring(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecurringScheduleRequest request) {
        ScheduleCreateResponse response = memberScheduleService.createRecurring(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-8 개인 일정 수정 */
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        memberScheduleService.update(memberId, scheduleId, request);
        return ResponseEntity.noContent().build();
    }

    /** 7-9 개인 일정 삭제 */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId) {
        memberScheduleService.delete(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}

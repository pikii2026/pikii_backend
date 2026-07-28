package com.pickii.domain.schedule.controller;

import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.dto.PartyScheduleAttendanceRequest;
import com.pickii.domain.schedule.dto.PartyScheduleRecurringRequest;
import com.pickii.domain.schedule.dto.PartyScheduleSingleRequest;
import com.pickii.domain.schedule.dto.PartyScheduleUpdateRequest;
import com.pickii.domain.schedule.dto.ScheduleCategorySelectRequest;
import com.pickii.domain.schedule.dto.ScheduleCreateResponse;
import com.pickii.domain.schedule.service.PartyScheduleService;
import com.pickii.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 팀 일정 조회/등록/수정/삭제, 팀원별 색상 지정 API (API_SPEC 7-15 ~ 7-19)
 */
@RestController
@RequiredArgsConstructor
public class PartyScheduleController {

    private final PartyScheduleService partyScheduleService;

    /** 7-15 팀 일정 조회 */
    @GetMapping("/projects/{projectId}/schedules")
    public ResponseEntity<ApiResponse<List<MyScheduleResponse>>> getMonthlySchedules(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @RequestParam int year,
            @RequestParam int month) {
        List<MyScheduleResponse> response = partyScheduleService.getMonthlySchedules(memberId, projectId, year, month);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 7-16 팀 일정 직접 등록 (단발) */
    @PostMapping("/projects/{projectId}/schedules/single")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createSingle(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody PartyScheduleSingleRequest request) {
        ScheduleCreateResponse response = partyScheduleService.createSingle(memberId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-16 팀 일정 직접 등록 (반복) */
    @PostMapping("/projects/{projectId}/schedules/recurring")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createRecurring(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody PartyScheduleRecurringRequest request) {
        ScheduleCreateResponse response = partyScheduleService.createRecurring(memberId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-17 팀 일정 수정 */
    @PatchMapping("/party-schedules/{scheduleId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody PartyScheduleUpdateRequest request) {
        partyScheduleService.update(memberId, scheduleId, request);
        return ResponseEntity.noContent().build();
    }

    /** 7-18 팀 일정 삭제 */
    @DeleteMapping("/party-schedules/{scheduleId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId) {
        partyScheduleService.delete(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    /** 7-20 회의 참석 / 불참 변경 */
    @PatchMapping("/party-schedules/{scheduleId}/attendance")
    public ResponseEntity<Void> updateAttendance(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody PartyScheduleAttendanceRequest request) {
        partyScheduleService.updateAttendance(memberId, scheduleId, request.attending());
        return ResponseEntity.noContent().build();
    }

    /** 7-19 프로젝트 색상(카테고리) 지정 - 개인별 */
    @PutMapping("/projects/{projectId}/schedule-category")
    public ResponseEntity<Void> selectCategory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody ScheduleCategorySelectRequest request) {
        partyScheduleService.selectCategory(memberId, projectId, request.categoryId());
        return ResponseEntity.noContent().build();
    }
}

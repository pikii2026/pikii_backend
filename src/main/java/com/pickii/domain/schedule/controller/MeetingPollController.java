package com.pickii.domain.schedule.controller;

import com.pickii.domain.schedule.dto.MeetingPollConfirmRequest;
import com.pickii.domain.schedule.dto.MeetingPollConfirmResponse;
import com.pickii.domain.schedule.dto.MeetingPollCreateRequest;
import com.pickii.domain.schedule.dto.MeetingPollCreateResponse;
import com.pickii.domain.schedule.dto.MeetingPollDetailResponse;
import com.pickii.domain.schedule.dto.MeetingPollResponseSubmitRequest;
import com.pickii.domain.schedule.dto.MeetingPollResponseSubmitResponse;
import com.pickii.domain.schedule.service.MeetingPollService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회의 조율 API (API_SPEC 7-10 ~ 7-14)
 */
@RestController
@RequiredArgsConstructor
public class MeetingPollController {

    private final MeetingPollService meetingPollService;

    /** 7-10 회의 조율 개설 */
    @PostMapping("/projects/{projectId}/meeting-polls")
    public ResponseEntity<ApiResponse<MeetingPollCreateResponse>> open(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody MeetingPollCreateRequest request) {
        MeetingPollCreateResponse response = meetingPollService.open(memberId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-11 응답 화면 조회 (슬롯 + 캘린더 프리필) */
    @GetMapping("/meeting-polls/{pollId}")
    public ResponseEntity<ApiResponse<MeetingPollDetailResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pollId) {
        MeetingPollDetailResponse response = meetingPollService.getDetail(memberId, pollId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 7-12 응답 제출 */
    @PostMapping("/meeting-polls/{pollId}/responses")
    public ResponseEntity<ApiResponse<MeetingPollResponseSubmitResponse>> submit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pollId,
            @Valid @RequestBody MeetingPollResponseSubmitRequest request) {
        MeetingPollResponseSubmitResponse response = meetingPollService.submit(memberId, pollId, request.unavailableSlotIds());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 7-13 최종 일정 확정 */
    @PatchMapping("/meeting-polls/{pollId}/confirm")
    public ResponseEntity<ApiResponse<MeetingPollConfirmResponse>> confirm(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pollId,
            @Valid @RequestBody MeetingPollConfirmRequest request) {
        MeetingPollConfirmResponse response = meetingPollService.confirm(memberId, pollId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 7-14 조율 취소 / 재조율 */
    @DeleteMapping("/meeting-polls/{pollId}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pollId) {
        meetingPollService.cancel(memberId, pollId);
        return ResponseEntity.noContent().build();
    }
}

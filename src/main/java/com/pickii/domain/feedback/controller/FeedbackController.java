package com.pickii.domain.feedback.controller;

import com.pickii.domain.feedback.dto.AiFeedbackDetailResponse;
import com.pickii.domain.feedback.dto.FeedbackCreateRequest;
import com.pickii.domain.feedback.dto.FeedbackListResponse;
import com.pickii.domain.feedback.dto.FeedbackTargetResponse;
import com.pickii.domain.feedback.service.FeedbackService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상호평가 / AI 피드백 API (API_SPEC 4-9~4-12)
 */
@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 4-9 평가 대상 팀원 조회 */
    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<FeedbackTargetResponse>> getEvaluationTargets(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        FeedbackTargetResponse response = feedbackService.getEvaluationTargets(memberId, projectId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-10 상호평가 작성 */
    @PostMapping
    public ResponseEntity<Void> write(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody FeedbackCreateRequest request) {
        feedbackService.write(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 4-11 AI 피드백 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackListResponse>>> getMyFeedbackList(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "joinedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FeedbackListResponse> response = feedbackService.getMyFeedbackList(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-12 AI 피드백 상세 조회 */
    @GetMapping("/ai/{projectId}")
    public ResponseEntity<ApiResponse<AiFeedbackDetailResponse>> getAiFeedbackDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        AiFeedbackDetailResponse response = feedbackService.getAiFeedbackDetail(memberId, projectId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

package com.pickii.domain.recruit.controller;

import com.pickii.domain.recruit.dto.AiDraftRequest;
import com.pickii.domain.recruit.dto.AiDraftResponse;
import com.pickii.domain.recruit.dto.CommentCreateRequest;
import com.pickii.domain.recruit.dto.CommentCreateResponse;
import com.pickii.domain.recruit.dto.CommentListResponse;
import com.pickii.domain.recruit.dto.RecruitCreateRequest;
import com.pickii.domain.recruit.dto.RecruitCreateResponse;
import com.pickii.domain.recruit.dto.RecruitDetailResponse;
import com.pickii.domain.recruit.dto.RecruitScrapResponse;
import com.pickii.domain.recruit.dto.RecruitSummaryResponse;
import com.pickii.domain.recruit.service.RecruitService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메인(Home) 및 공고 API (API_SPEC 2., 3.)
 */
@RestController
@RequestMapping("/recruits")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;

    /** 2-1 공고 검색 및 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecruitSummaryResponse>>> searchRecruits(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onCampus,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> topicIds,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<RecruitSummaryResponse> response =
                recruitService.searchRecruits(keyword, onCampus, categoryIds, topicIds, memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-1 공고 상세 조회 */
    @GetMapping("/{recruitId}")
    public ResponseEntity<ApiResponse<RecruitDetailResponse>> getRecruitDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        RecruitDetailResponse response = recruitService.getRecruitDetail(recruitId, memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-2 공고 작성 */
    @PostMapping
    public ResponseEntity<ApiResponse<RecruitCreateResponse>> createRecruit(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecruitCreateRequest request) {
        RecruitCreateResponse response = recruitService.createRecruit(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 3-3 AI 공고 초안 생성 */
    @PostMapping("/ai-draft")
    public ResponseEntity<ApiResponse<AiDraftResponse>> generateAiDraft(@Valid @RequestBody AiDraftRequest request) {
        AiDraftResponse response = recruitService.generateAiDraft(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-4 공고 마감 */
    @PatchMapping("/{recruitId}/close")
    public ResponseEntity<Void> closeRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        recruitService.closeRecruit(memberId, recruitId);
        return ResponseEntity.noContent().build();
    }

    /** 3-5 공고 추가 모집 */
    @PatchMapping("/{recruitId}/additional")
    public ResponseEntity<Void> openAdditionalRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        recruitService.openAdditionalRecruit(memberId, recruitId);
        return ResponseEntity.noContent().build();
    }

    /** 3-6 공고 삭제 */
    @DeleteMapping("/{recruitId}")
    public ResponseEntity<Void> deleteRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        recruitService.deleteRecruit(memberId, recruitId);
        return ResponseEntity.noContent().build();
    }

    /** 3-7 댓글 및 답글 목록 조회 */
    @GetMapping("/{recruitId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getComments(@PathVariable Long recruitId) {
        CommentListResponse response = recruitService.getComments(recruitId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-8 댓글 및 답글 작성 */
    @PostMapping("/{recruitId}/comments")
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentCreateResponse response = recruitService.createComment(memberId, recruitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 3-12 공고 수정 */
    @PatchMapping("/{recruitId}")
    public ResponseEntity<Void> updateRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId,
            @Valid @RequestBody RecruitCreateRequest request) {
        recruitService.updateRecruit(memberId, recruitId, request);
        return ResponseEntity.noContent().build();
    }

    /** 3-14 공고 스크랩 */
    @PostMapping("/{recruitId}/scrap")
    public ResponseEntity<ApiResponse<RecruitScrapResponse>> scrapRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        RecruitScrapResponse response = recruitService.scrapRecruit(memberId, recruitId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 3-15 공고 스크랩 취소 */
    @DeleteMapping("/{recruitId}/scrap")
    public ResponseEntity<Void> cancelScrapRecruit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId) {
        recruitService.cancelScrapRecruit(memberId, recruitId);
        return ResponseEntity.noContent().build();
    }
}

package com.pickii.domain.apply.controller;

import com.pickii.domain.apply.dto.ApplyAiDraftRequest;
import com.pickii.domain.apply.dto.ApplyAiDraftResponse;
import com.pickii.domain.apply.dto.ApplyCreateRequest;
import com.pickii.domain.apply.service.ApplyService;
import com.pickii.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공고 지원 API (API_SPEC 3.)
 */
@RestController
@RequestMapping("/recruits/{recruitId}/applies")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    /** 3-10 AI 지원서 초안 생성 */
    @PostMapping("/ai-draft")
    public ResponseEntity<ApiResponse<ApplyAiDraftResponse>> generateAiDraft(@Valid @RequestBody ApplyAiDraftRequest request) {
        ApplyAiDraftResponse response = applyService.generateAiDraft(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-11 공고 지원하기 */
    @PostMapping
    public ResponseEntity<Void> apply(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId,
            @Valid @RequestBody ApplyCreateRequest request) {
        applyService.apply(memberId, recruitId, request);
        return ResponseEntity.noContent().build();
    }
}

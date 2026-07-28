package com.pickii.domain.apply.controller;

import com.pickii.domain.apply.dto.ApplicantResponse;
import com.pickii.domain.apply.service.ApplyService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지원자 목록 API (API_SPEC 4-7)
 */
@RestController
@RequestMapping("/recruits/{recruitId}/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplyService applyService;

    /** 4-7 지원자 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApplicantResponse>>> getApplicants(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ApplicantResponse> response = applyService.getApplicants(memberId, recruitId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

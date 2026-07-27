package com.pickii.domain.recruit.controller;

import com.pickii.domain.recruit.dto.RecruitSummaryResponse;
import com.pickii.domain.recruit.service.RecruitService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메인(Home) API (API_SPEC 2.)
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
}

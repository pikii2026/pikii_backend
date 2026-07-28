package com.pickii.domain.member.controller;

import com.pickii.domain.member.dto.UnivResponse;
import com.pickii.domain.member.service.UnivService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Data: 대학교 목록 (API_SPEC 5-8)
 */
@RestController
@RequiredArgsConstructor
public class UnivController {

    private final UnivService univService;

    /** 5-8 대학교 목록 조회 */
    @GetMapping("/universities")
    public ResponseEntity<ApiResponse<List<UnivResponse>>> getUnivs(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.of(univService.getUnivs(keyword)));
    }
}

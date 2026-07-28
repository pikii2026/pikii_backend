package com.pickii.domain.resume.controller;

import com.pickii.domain.resume.dto.LicenseResponse;
import com.pickii.domain.resume.dto.LinkCategoryResponse;
import com.pickii.domain.resume.dto.TechStackResponse;
import com.pickii.domain.resume.service.ResumeMasterDataService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Data: 기술스택/자격증/링크카테고리 (API_SPEC 5-3~5-5)
 */
@RestController
@RequiredArgsConstructor
public class ResumeMasterDataController {

    private final ResumeMasterDataService resumeMasterDataService;

    /** 5-3 기술 스택 조회 */
    @GetMapping("/tech-stacks")
    public ResponseEntity<ApiResponse<List<TechStackResponse>>> getTechStacks() {
        return ResponseEntity.ok(ApiResponse.of(resumeMasterDataService.getTechStacks()));
    }

    /** 5-4 자격증 조회 */
    @GetMapping("/licenses")
    public ResponseEntity<ApiResponse<List<LicenseResponse>>> getLicenses() {
        return ResponseEntity.ok(ApiResponse.of(resumeMasterDataService.getLicenses()));
    }

    /** 5-5 링크 카테고리 조회 */
    @GetMapping("/link-categories")
    public ResponseEntity<ApiResponse<List<LinkCategoryResponse>>> getLinkCategories() {
        return ResponseEntity.ok(ApiResponse.of(resumeMasterDataService.getLinkCategories()));
    }
}

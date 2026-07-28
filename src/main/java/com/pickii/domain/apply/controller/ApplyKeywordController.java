package com.pickii.domain.apply.controller;

import com.pickii.domain.apply.dto.ApplyKeywordCategoryResponse;
import com.pickii.domain.apply.service.ApplyService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Data: 지원 키워드 (API_SPEC 5-7)
 */
@RestController
@RequiredArgsConstructor
public class ApplyKeywordController {

    private final ApplyService applyService;

    /** 5-7 지원 키워드 조회 */
    @GetMapping("/apply-keywords")
    public ResponseEntity<ApiResponse<List<ApplyKeywordCategoryResponse>>> getApplyKeywords() {
        return ResponseEntity.ok(ApiResponse.of(applyService.getApplyKeywords()));
    }
}

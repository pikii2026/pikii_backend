package com.pickii.domain.recruit.controller;

import com.pickii.domain.recruit.dto.CategoryResponse;
import com.pickii.domain.recruit.dto.TopicResponse;
import com.pickii.domain.recruit.service.RecruitMasterDataService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Data: 카테고리/주제 (API_SPEC 5-1, 5-2)
 */
@RestController
@RequiredArgsConstructor
public class RecruitMasterDataController {

    private final RecruitMasterDataService recruitMasterDataService;

    /** 5-1 카테고리 조회 */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.of(recruitMasterDataService.getCategories()));
    }

    /** 5-2 주제 조회 */
    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopics() {
        return ResponseEntity.ok(ApiResponse.of(recruitMasterDataService.getTopics()));
    }
}

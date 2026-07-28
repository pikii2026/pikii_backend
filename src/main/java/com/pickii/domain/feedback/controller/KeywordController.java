package com.pickii.domain.feedback.controller;

import com.pickii.domain.feedback.dto.KeywordResponse;
import com.pickii.domain.feedback.service.FeedbackService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Data: 피드백 키워드 (API_SPEC 5-6)
 */
@RestController
@RequiredArgsConstructor
public class KeywordController {

    private final FeedbackService feedbackService;

    /** 5-6 피드백 키워드 조회 */
    @GetMapping("/keywords")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getKeywords() {
        return ResponseEntity.ok(ApiResponse.of(feedbackService.getKeywords()));
    }
}

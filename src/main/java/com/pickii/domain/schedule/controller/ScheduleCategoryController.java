package com.pickii.domain.schedule.controller;

import com.pickii.domain.schedule.dto.ScheduleCategoryCreateResponse;
import com.pickii.domain.schedule.dto.ScheduleCategoryRequest;
import com.pickii.domain.schedule.dto.ScheduleCategoryResponse;
import com.pickii.domain.schedule.service.ScheduleCategoryService;
import com.pickii.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 일정 카테고리 API (API_SPEC 7-1 ~ 7-4)
 */
@RestController
@RequestMapping("/users/me/schedule-categories")
@RequiredArgsConstructor
public class ScheduleCategoryController {

    private final ScheduleCategoryService scheduleCategoryService;

    /** 7-1 일정 카테고리 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleCategoryResponse>>> getCategories(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.of(scheduleCategoryService.getCategories(memberId)));
    }

    /** 7-2 일정 카테고리 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCategoryCreateResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ScheduleCategoryRequest request) {
        ScheduleCategoryCreateResponse response = scheduleCategoryService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 7-3 일정 카테고리 수정 */
    @PatchMapping("/{categoryId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long categoryId,
            @Valid @RequestBody ScheduleCategoryRequest request) {
        scheduleCategoryService.update(memberId, categoryId, request);
        return ResponseEntity.noContent().build();
    }

    /** 7-4 일정 카테고리 삭제 */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long categoryId) {
        scheduleCategoryService.delete(memberId, categoryId);
        return ResponseEntity.noContent().build();
    }
}

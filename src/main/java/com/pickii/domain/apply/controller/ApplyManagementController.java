package com.pickii.domain.apply.controller;

import com.pickii.domain.apply.dto.ApplyStatusUpdateRequest;
import com.pickii.domain.apply.service.ApplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지원서 관리 API (API_SPEC 3., 4-8)
 */
@RestController
@RequestMapping("/applies")
@RequiredArgsConstructor
public class ApplyManagementController {

    private final ApplyService applyService;

    /** 3-13 지원 취소 */
    @DeleteMapping("/{applyId}")
    public ResponseEntity<Void> cancelApply(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long applyId) {
        applyService.cancelApply(memberId, applyId);
        return ResponseEntity.noContent().build();
    }

    /** 4-8 지원자 수락/거절 */
    @PatchMapping("/{applyId}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long applyId,
            @Valid @RequestBody ApplyStatusUpdateRequest request) {
        applyService.updateStatus(memberId, applyId, request);
        return ResponseEntity.noContent().build();
    }
}

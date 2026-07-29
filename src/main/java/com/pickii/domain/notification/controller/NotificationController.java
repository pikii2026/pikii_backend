package com.pickii.domain.notification.controller;

import com.pickii.domain.notification.dto.NotificationResponse;
import com.pickii.domain.notification.dto.UnreadCountResponse;
import com.pickii.domain.notification.service.NotificationService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API (API_SPEC 9-1~9-4, 9-7)
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 9-1 알림 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<NotificationResponse> response = notificationService.getNotifications(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 9-2 알림 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long notificationId) {
        notificationService.markRead(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** 9-3 전체 읽음 처리 */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Long memberId) {
        notificationService.markAllRead(memberId);
        return ResponseEntity.noContent().build();
    }

    /** 9-4 알림 삭제 */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long notificationId) {
        notificationService.delete(memberId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** 9-7 안 읽은 알림 개수 조회 */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@AuthenticationPrincipal Long memberId) {
        UnreadCountResponse response = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

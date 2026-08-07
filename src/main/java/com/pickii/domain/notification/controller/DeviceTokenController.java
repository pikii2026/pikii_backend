package com.pickii.domain.notification.controller;

import com.pickii.domain.notification.dto.DeviceTokenDeleteRequest;
import com.pickii.domain.notification.dto.DeviceTokenRegisterRequest;
import com.pickii.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시스템 푸시(FCM)용 디바이스 토큰 등록/삭제 API (API_SPEC 9-8~9-9)
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final NotificationService notificationService;

    /** 9-8 디바이스 토큰 등록 */
    @PostMapping
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody DeviceTokenRegisterRequest request) {
        notificationService.registerDeviceToken(memberId, request);
        return ResponseEntity.noContent().build();
    }

    /** 9-9 디바이스 토큰 삭제 */
    @DeleteMapping
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody DeviceTokenDeleteRequest request) {
        notificationService.unregisterDeviceToken(memberId, request.fcmToken());
        return ResponseEntity.noContent().build();
    }
}

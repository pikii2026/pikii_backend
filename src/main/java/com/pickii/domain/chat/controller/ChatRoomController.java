package com.pickii.domain.chat.controller;

import com.pickii.domain.chat.dto.ChatMessagePageResponse;
import com.pickii.domain.chat.dto.ChatRoomDetailResponse;
import com.pickii.domain.chat.dto.ChatRoomListItem;
import com.pickii.domain.chat.dto.ChatRoomNotificationRequest;
import com.pickii.domain.chat.dto.ChatRoomReadRequest;
import com.pickii.domain.chat.dto.DirectChatRoomRequest;
import com.pickii.domain.chat.dto.DirectChatRoomResponse;
import com.pickii.domain.chat.entity.ChatRoomType;
import com.pickii.domain.chat.service.ChatRoomService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 API (API_SPEC 8-1, 8-2, 8-3, 8-5, 8-6, 8-7, 8-8)
 */
@RestController
@RequestMapping("/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /** 8-1 채팅방 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChatRoomListItem>>> getChatRooms(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) ChatRoomType type,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ChatRoomListItem> response = chatRoomService.getChatRooms(memberId, type, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 8-2 채팅방 상세 */
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId) {
        ChatRoomDetailResponse response = chatRoomService.getDetail(memberId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 8-3 이전 채팅 조회 */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        ChatMessagePageResponse response = chatRoomService.getMessages(memberId, chatRoomId, cursor, size);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 8-5 1:1 채팅방 생성 */
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<DirectChatRoomResponse>> createDirectRoom(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody DirectChatRoomRequest request) {
        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(memberId, request.targetMemberId());
        HttpStatus status = response.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.of(response));
    }

    /** 8-6 채팅방 읽음 처리 */
    @PatchMapping("/{chatRoomId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomReadRequest request) {
        chatRoomService.markRead(memberId, chatRoomId, request.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }

    /** 8-7 채팅방 나가기 */
    @DeleteMapping("/{chatRoomId}/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId) {
        chatRoomService.leave(memberId, chatRoomId);
        return ResponseEntity.noContent().build();
    }

    /** 8-8 채팅방 알림 설정 */
    @PatchMapping("/{chatRoomId}/notification")
    public ResponseEntity<Void> updateNotification(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomNotificationRequest request) {
        chatRoomService.updateNotification(memberId, chatRoomId, request.enabled());
        return ResponseEntity.noContent().build();
    }
}

package com.pickii.domain.chat.controller;

import com.pickii.domain.chat.dto.ChatImageUploadResponse;
import com.pickii.domain.chat.service.ChatImageService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 채팅 이미지 업로드 API (API_SPEC 8-4)
 */
@RestController
@RequestMapping("/chatrooms/{chatRoomId}/images")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatImageService chatImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatImageUploadResponse>> upload(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatRoomId,
            @RequestParam("image") MultipartFile image) {
        ChatImageUploadResponse response = chatImageService.upload(memberId, chatRoomId, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}

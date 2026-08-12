package com.pickii.domain.chat.controller;

import com.pickii.domain.chat.service.ChatImageService;
import com.pickii.global.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 채팅 이미지 조회 (API_SPEC 8-4에서 발급한 URL).
 *
 * <p>오브젝트 스토리지는 비공개라 공개 URL이 없으므로, 서버가 객체를 읽어 전달한다.
 * 파일명이 UUID라 내용이 바뀌지 않으므로 오래 캐시한다.</p>
 */
@RestController
@RequiredArgsConstructor
public class ChatImageViewController {

    private final ImageStorage imageStorage;

    @GetMapping(ChatImageService.URL_PREFIX + "/{year}/{month}/{filename}")
    public ResponseEntity<byte[]> get(
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String filename) {
        String key = "%s/%s/%s/%s".formatted(ChatImageService.KEY_PREFIX, year, month, filename);
        ImageStorage.StoredImage image = imageStorage.load(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.content());
    }
}

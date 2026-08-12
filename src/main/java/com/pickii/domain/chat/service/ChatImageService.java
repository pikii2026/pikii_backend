package com.pickii.domain.chat.service;

import com.pickii.domain.chat.dto.ChatImageUploadResponse;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import com.pickii.global.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * API_SPEC 8-4 채팅 이미지 업로드.
 *
 * <p>저장은 {@link ImageStorage}에 위임한다(로컬은 파일시스템, 배포 환경은 오브젝트 스토리지).
 * 스토리지가 비공개이므로 공개 URL 대신 서버가 프록시하는 경로를 반환한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatImageService {

    private static final List<String> ALLOWED_TYPES = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** 저장 키 접두어 및 조회 경로 (SecurityConfig의 permitAll 경로와 일치해야 한다) */
    public static final String KEY_PREFIX = "chat";
    public static final String URL_PREFIX = "/chat-images";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ImageStorage imageStorage;

    public ChatImageUploadResponse upload(Long memberId, Long chatRoomId, MultipartFile image) {
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String ext = extractExtension(image.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String path = YearMonth.now().toString().replace("-", "/") + "/" + UUID.randomUUID() + "." + ext;
        byte[] content;
        try {
            content = image.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        imageStorage.store(KEY_PREFIX + "/" + path, content, contentType(ext));

        return new ChatImageUploadResponse(URL_PREFIX + "/" + path);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String contentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}

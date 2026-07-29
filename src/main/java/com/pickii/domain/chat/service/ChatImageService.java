package com.pickii.domain.chat.service;

import com.pickii.domain.chat.dto.ChatImageUploadResponse;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * API_SPEC 8-4 채팅 이미지 업로드.
 * TODO: 실제 Object Storage(S3) 연동 전까지는 로컬 파일시스템 저장으로 목업한다.
 */
@Service
@RequiredArgsConstructor
public class ChatImageService {

    private static final List<String> ALLOWED_TYPES = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Path UPLOAD_ROOT = Path.of("uploads/chat");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatImageUploadResponse upload(Long memberId, Long chatRoomId, MultipartFile image) {
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String ext = extractExtension(image.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String yearMonth = YearMonth.now().toString().replace("-", "/");
        String filename = UUID.randomUUID() + "." + ext;
        Path targetDir = UPLOAD_ROOT.resolve(yearMonth);
        Path targetPath = targetDir.resolve(filename);
        try {
            Files.createDirectories(targetDir);
            image.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String imageUrl = "/static-uploads/chat/" + yearMonth + "/" + filename;
        return new ChatImageUploadResponse(imageUrl);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}

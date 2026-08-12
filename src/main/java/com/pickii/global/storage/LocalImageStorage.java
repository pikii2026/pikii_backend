package com.pickii.global.storage;

import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 파일시스템 기반 이미지 저장소. 로컬 개발 기본값.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private final StorageProperties properties;

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = root().resolve(key).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public StoredImage load(String key) {
        Path target = root().resolve(key).normalize();
        if (!target.startsWith(root()) || !Files.exists(target)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        try {
            String contentType = Files.probeContentType(target);
            return new StoredImage(Files.readAllBytes(target),
                    contentType != null ? contentType : "application/octet-stream");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private Path root() {
        return Path.of(properties.localPath()).toAbsolutePath().normalize();
    }
}

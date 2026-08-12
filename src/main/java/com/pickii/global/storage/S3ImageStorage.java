package com.pickii.global.storage;

import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

/**
 * S3 호환 오브젝트 스토리지(Railway Buckets) 기반 이미지 저장소.
 *
 * <p>버킷은 비공개이므로 클라이언트에 공개 URL을 주지 않는다.
 * 서버가 객체를 읽어 프록시로 전달한다({@code /chat-images/**}).</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {

    private final S3Client client;
    private final String bucket;

    public S3ImageStorage(StorageProperties properties) {
        this.bucket = properties.bucket();
        this.client = S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .build();
        log.info("오브젝트 스토리지 연동 완료 - bucket={}, endpoint={}", bucket, properties.endpoint());
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.warn("오브젝트 스토리지 업로드 실패 - key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public StoredImage load(String key) {
        try {
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            String contentType = object.response().contentType();
            return new StoredImage(object.asByteArray(),
                    contentType != null ? contentType : "application/octet-stream");
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        } catch (S3Exception e) {
            log.warn("오브젝트 스토리지 조회 실패 - key={}", key, e);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}

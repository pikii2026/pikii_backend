package com.pickii.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 app.storage.* 설정 바인딩 (업로드 이미지 저장소)
 *
 * @param provider  local(기본, 파일시스템) | s3(Railway Buckets 등 S3 호환)
 * @param localPath provider=local 일 때 저장 루트 경로
 * @param bucket    S3 버킷 이름 (환경변수 AWS_S3_BUCKET_NAME)
 * @param endpoint  S3 API 엔드포인트 (환경변수 AWS_ENDPOINT_URL)
 * @param region    S3 리전 (환경변수 AWS_DEFAULT_REGION, Railway는 auto)
 * @param accessKey S3 액세스 키 (환경변수 AWS_ACCESS_KEY_ID)
 * @param secretKey S3 시크릿 키 (환경변수 AWS_SECRET_ACCESS_KEY)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        String localPath,
        String bucket,
        String endpoint,
        String region,
        String accessKey,
        String secretKey
) {
}

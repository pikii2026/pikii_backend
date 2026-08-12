package com.pickii.global.storage;

/**
 * 업로드 이미지 저장소 추상화.
 *
 * <p>로컬 개발은 파일시스템, 배포 환경은 S3 호환 오브젝트 스토리지(Railway Buckets)를 사용한다.
 * 배포 환경의 컨테이너 파일시스템은 재배포 시 초기화되므로 로컬 저장을 쓸 수 없다.
 * 구현체는 app.storage.provider 값으로 선택된다.</p>
 */
public interface ImageStorage {

    /** key 위치에 이미지를 저장한다. */
    void store(String key, byte[] content, String contentType);

    /** key에 저장된 이미지를 읽는다. 없으면 예외를 던진다. */
    StoredImage load(String key);

    record StoredImage(byte[] content, String contentType) {
    }
}

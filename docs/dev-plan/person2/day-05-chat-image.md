# Day 5 (반나절) — 채팅 이미지 업로드 (8-4)

## ⚠️ 스코프 판단이 필요한 날입니다

스펙에는 "Object Storage(AWS S3)에 저장"이라고 되어 있는데, **현재 프로젝트에 AWS SDK 의존성도, S3 설정도 전혀 없습니다.** 12일 안에 실제 AWS 계정 발급 + 버킷 생성 + IAM 권한 설정까지 하는 건 오늘 반나절 스코프를 크게 초과합니다.

**추천: 오늘은 로컬 파일 시스템 저장으로 목업 구현**하고, 나중에 시간이 되면 S3로 교체하세요. API 스펙(요청/응답 형태)은 완전히 동일하게 유지하면 나중에 저장소만 바꿔치기하면 됩니다.

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 8-4 채팅 이미지 업로드 | `POST /chatrooms/{chatRoomId}/images` (multipart) | 201, imageUrl 반환 (로컬 저장 기준) |

---

## 사전 확인 사항

- `spring-boot-starter-web`에 멀티파트 업로드 처리가 기본 포함되어 있어서 별도 의존성 추가는 필요 없습니다.
- `application.yaml`에 업로드 크기 제한 설정 확인/추가:
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 10MB
        max-request-size: 10MB
  ```

---

## 구현 순서

```java
@Service
@RequiredArgsConstructor
public class ChatImageService {

    private static final List<String> ALLOWED_TYPES = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Path UPLOAD_ROOT = Path.of("uploads/chat"); // 로컬 저장 경로 (목업)

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatImageUploadResponse upload(Long memberId, Long chatRoomId, MultipartFile file) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String yearMonth = YearMonth.now().toString().replace("-", "/"); // "2026/07"
        String filename = UUID.randomUUID() + "." + ext;
        Path targetDir = UPLOAD_ROOT.resolve(yearMonth);
        Path targetPath = targetDir.resolve(filename);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        // 실제 CDN 대신 로컬 정적 리소스 URL로 목업 (아래 WebConfig 참고)
        String imageUrl = "/static-uploads/chat/" + yearMonth + "/" + filename;
        return new ChatImageUploadResponse(imageUrl);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
```

로컬에 저장한 파일을 실제로 URL로 접근 가능하게 하려면 정적 리소스 매핑이 필요합니다:
```java
// global/config/WebConfig.java (신규)
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static-uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

Controller:
```java
@PostMapping(value = "/chatrooms/{chatRoomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<ChatImageUploadResponse>> upload(
        @PathVariable Long chatRoomId, @RequestParam("image") MultipartFile image,
        @AuthenticationPrincipal Long memberId) {
    var response = chatImageService.upload(memberId, chatRoomId, image);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
}
```

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 허용 안 된 확장자 | 400 `INVALID_FILE_TYPE` |
| 채팅방 참여자 아님 | 403 `FORBIDDEN` |
| 존재하지 않는 채팅방 | 404 `CHATROOM_NOT_FOUND` |
| 10MB 초과 | 413 `FILE_TOO_LARGE` |
| 저장 실패 | 500 `FILE_UPLOAD_FAILED` |

---

## 테스트 체크리스트

- [ ] jpg/png 파일 업로드 → 201 + imageUrl 반환
- [ ] 반환된 imageUrl로 실제 브라우저에서 이미지가 열리는지 확인
- [ ] txt, exe 등 허용 안 된 확장자 업로드 시도 → 400
- [ ] 10MB 넘는 파일 업로드 시도 → 413
- [ ] 채팅방 참여자 아닌 계정으로 업로드 시도 → 403

---

## 커밋/PR 가이드

- 브랜치: `feat/chat-image`
- 커밋: `feat: 채팅 이미지 업로드 API 구현 (8-4, 로컬 저장 목업)`
- 커밋 메시지나 PR 설명에 **"S3 대신 로컬 파일시스템으로 임시 구현했다"는 점을 꼭 남겨두세요** — 나중에 배포 환경에서 헷갈리지 않도록.

---

## 막힐 수 있는 포인트

- **로컬 파일은 서버 재시작/재배포 시 날아갈 수 있습니다.** 데모/제출 전에는 문제없지만, 실제 배포한다면 이 시점에 S3로 교체가 필요하다는 걸 기억해두세요(TODO 주석 남기기).
- **`uploads/` 폴더가 git에 커밋되지 않게** `.gitignore`에 추가하세요.
- **멀티파트 요청이 Swagger UI에서 파일 업로드 폼으로 안 뜨면**, springdoc 설정에 `@Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))` 어노테이션이 필요할 수 있습니다. 안 되면 curl `-F "image=@파일경로"`로 직접 테스트하세요.

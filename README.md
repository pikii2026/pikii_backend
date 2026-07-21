# Pickii Backend

Pickii 백엔드 서버 (Spring Boot 3.5 / Java 17)

## 기술 스택

| 구분 | 기술 |
|:---|:---|
| Framework | Spring Boot 3.5 (Web MVC, Security, WebSocket) |
| DB | MySQL 8.0 (메인), MongoDB 7 (채팅), Redis 7 (인증/세션) |
| ORM | Spring Data JPA / Spring Data MongoDB |
| 인증 | JWT (jjwt) + Refresh Token Rotation |
| API 문서 | springdoc (Swagger UI) |

## 개발 환경 세팅 (팀원 온보딩)

사전 준비물: **JDK 17**, **Docker Desktop**, IntelliJ

```bash
# 1. 클론
git clone <repo-url>
cd Pickii

# 2. 환경변수 파일 생성 후 값 채우기 (JWT_SECRET 등은 팀 공유 값 사용)
cp .env.example .env

# 3. 인프라(MySQL + Redis + MongoDB) 실행
docker compose up -d

# 4. 실행 확인
docker compose ps
```

이후 IntelliJ에서 `PickiiApplication` 실행 (기본 프로필이 `local`이라 별도 설정 불필요).

- 서버: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html

메일 발송 기능을 테스트하려면 IntelliJ 실행 구성(Run Configuration)의
환경변수에 `MAIL_USERNAME`, `MAIL_PASSWORD`(Gmail 앱 비밀번호)를 등록한다.

## 인프라 관리 명령어

```bash
docker compose up -d      # 시작
docker compose stop       # 중지 (데이터 유지)
docker compose down       # 컨테이너 삭제 (데이터 유지)
docker compose down -v    # 컨테이너 + 데이터 완전 삭제
```

## 패키지 구조

```
com.pickii
├── global                  # 전 도메인 공통
│   ├── common/response     # ApiResponse, PageResponse (공통 응답 형식)
│   ├── exception           # ErrorCode, BusinessException, 전역 예외 처리
│   ├── security            # SecurityConfig, JWT 발급/검증/필터
│   ├── config              # Swagger, JPA Auditing
│   └── entity              # BaseTimeEntity (createdAt/updatedAt)
└── domain                  # 도메인별 entity / repository (+ service, controller, dto 추가 예정)
    ├── member              # 회원, 소셜 연동, 대학교
    ├── resume              # 이력서(프로필), 기술스택, 자격증, 링크
    ├── recruit             # 공고, 카테고리/주제, 댓글, 스크랩
    ├── apply               # 지원서, 지원 키워드
    ├── project             # 프로젝트, 팀원
    ├── schedule            # 개인/팀 일정, 회의 조율(MeetingPoll)
    ├── chat                # 채팅방(MySQL) + 메시지(MongoDB)
    ├── notification        # 알림 설정, 알림 내역
    ├── feedback            # 상호평가, AI 피드백
    └── auth                # 인증 Enum, Redis Key 유틸 (엔티티 없음)
```

각 도메인 작업 시 `entity`/`repository` 옆에 `service`, `controller`, `dto` 패키지를 만들어 구현한다.

- 비즈니스 예외는 `throw new BusinessException(ErrorCode.XXX)` 로 던지면 공통 에러 형식으로 응답된다.
- 성공 응답은 `ApiResponse.of(data)`, 목록은 `PageResponse.from(page)` 를 사용한다.
- 엔티티는 `BaseTimeEntity` 상속 시 생성/수정 시각이 자동 기록된다.

## 문서

| 문서 | 설명 |
|:---|:---|
| [docs/DOCKER_GUIDE.md](docs/DOCKER_GUIDE.md) | Docker 개발환경 구성/실행/트러블슈팅 가이드 |
| [docs/API_SPEC.md](docs/API_SPEC.md) | REST API 명세 |
| [docs/DB_Schema.md](docs/DB_Schema.md) | DB 스키마 / ERD |
| [docs/Redis_Policy.md](docs/Redis_Policy.md) | Redis 인증 정책 |
| [docs/ERROR_CODE.md](docs/ERROR_CODE.md) | 에러 코드 정의 |
| [docs/ENUM.md](docs/ENUM.md) | Enum 정의 |

---

# 개발 방식

## 1. 기능 하나를 구현하는 순서

모든 기능은 아래 순서로 진행한다. **문서가 먼저, 코드가 나중이다.**

```
① docs/API_SPEC.md 에서 담당 API 스펙 확인
   (URL, Request/Response 형식, Business Logic, Error Code)

② main 최신화 후 브랜치 생성
   git checkout main && git pull
   git checkout -b feat/기능명

③ 도메인 패키지에 dto → service → controller 순서로 구현
   (entity/repository 뼈대는 이미 있음. 필요 시 메서드 추가)

④ 앱 실행 후 Swagger에서 직접 호출해 스펙과 응답이 일치하는지 확인
   http://localhost:8080/api/v1/swagger-ui/index.html

⑤ 커밋 → push → GitHub에서 PR 생성 → 리뷰 후 main 머지
```

스펙과 다르게 구현해야 하는 상황이 생기면 **먼저 팀에 공유하고 docs 문서를 수정한 뒤** 코드를 작성한다. (문서와 코드가 어긋나면 프론트가 고통받는다)

## 2. 브랜치 & 커밋 규칙

### 브랜치

| 브랜치 | 용도 |
|:---|:---|
| `main` | 항상 실행 가능한 상태 유지. **직접 push 금지, PR로만 머지** |
| `feat/기능명` | 기능 개발 (예: `feat/signup`, `feat/recruit-list`) |
| `fix/버그명` | 버그 수정 (예: `fix/login-401`) |

### 커밋 메시지

```
feat: 회원가입 API 구현
fix: 로그인 시 LastLoginAt 갱신 안 되던 문제 수정
docs: API_SPEC 지원 취소 에러코드 추가
refactor: RecruitService 검색 조건 분리
```

- 접두어: `feat` / `fix` / `docs` / `refactor` / `test` / `chore`
- 한 커밋에는 한 가지 변경만. "이것저것 수정"같은 커밋 금지

### PR

- 제목은 커밋 컨벤션과 동일하게, 본문에 **무엇을/왜** 간단히
- 관련 API 번호를 적으면 좋음 (예: `API_SPEC 1-4 회원가입`)
- 다른 팀원 1명 이상 승인 후 머지

## 3. 레이어별 역할 (반드시 지키기)

```
Controller  →  Service  →  Repository
(요청/응답)    (비즈니스 로직)   (DB 접근)
```

| 레이어 | 하는 일 | 하면 안 되는 일 |
|:---|:---|:---|
| **Controller** | 요청 DTO 받기, `@Valid` 검증, Service 호출, `ApiResponse`로 감싸 반환 | 비즈니스 로직, Repository 직접 호출 |
| **Service** | 비즈니스 로직, 트랜잭션(`@Transactional`), 예외 던지기 | HttpServletRequest 등 웹 계층 의존 |
| **Repository** | 쿼리 메서드 정의 | 비즈니스 판단 |
| **DTO** | 요청/응답 데이터 전달 (record 사용) | 엔티티를 그대로 응답으로 노출 ❌ |

## 4. 코드 컨벤션

- **DTO는 `record`로**, 요청 DTO 검증은 `@NotBlank`, `@Size` 등 Bean Validation 사용
- **엔티티에 Setter 금지.** 상태 변경은 의미 있는 메서드로 (`recruit.close()`, `apply.accept()`)
- 엔티티 생성은 `@Builder` 또는 생성자로, `BaseTimeEntity` 상속 시 생성/수정 시각 자동 기록
- **비즈니스 예외는 `throw new BusinessException(ErrorCode.XXX)`** — 새 에러가 필요하면 `ErrorCode` enum과 `docs/ERROR_CODE.md`에 먼저 추가
- 성공 응답은 `ApiResponse.of(data)`, 목록은 `PageResponse.from(page)` — 형식을 직접 만들지 않는다
- 로그인 사용자의 memberId는 `@AuthenticationPrincipal Long memberId`로 받는다
- Service의 조회 메서드에는 `@Transactional(readOnly = true)`

### 예시 (이 패턴을 그대로 복사해서 시작)

```java
// domain/recruit/dto/RecruitCreateRequest.java
public record RecruitCreateRequest(
        @NotBlank @Size(min = 2, max = 20) String title,
        @Min(1) @Max(7) int maxMembers,
        @NotNull Boolean onCampus,
        @NotBlank @Size(max = 1000) String content
        /* ... */
) {}

// domain/recruit/controller/RecruitController.java
@RestController
@RequestMapping("/recruits")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecruitCreateResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecruitCreateRequest request) {
        RecruitCreateResponse response = recruitService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}

// domain/recruit/service/RecruitService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitService {

    private final RecruitRepository recruitRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RecruitCreateResponse create(Long memberId, RecruitCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Recruit recruit = Recruit.builder()
                .member(member)
                .title(request.title())
                /* ... */
                .build();
        return new RecruitCreateResponse(recruitRepository.save(recruit).getId());
    }
}
```

## 5. 인증이 필요한 API 개발하기

- 인증 불필요 API는 `SecurityConfig`의 `permitAll()` 목록에 등록되어 있다 (`/auth/**`, 공고 조회 등). **새 공개 API를 만들면 여기에 추가**해야 401이 안 난다.
- 그 외 API는 자동으로 인증 필요. 요청 시 `Authorization: Bearer {accessToken}` 헤더 필수
- Swagger에서 테스트할 때는 우측 상단 **Authorize** 버튼에 토큰을 넣으면 이후 요청에 자동 포함된다

## 6. 문서 수정 규칙

코드보다 문서를 먼저 고친다.

| 변경 내용 | 수정할 문서 |
|:---|:---|
| API 추가/변경 | docs/API_SPEC.md |
| 에러 코드 추가 | docs/ERROR_CODE.md + `ErrorCode` enum |
| Enum 추가 | docs/ENUM.md + 해당 도메인 enum 클래스 |
| 테이블/컬럼 변경 | docs/DB_Schema.md + 엔티티 |
| Redis Key 추가 | docs/Redis_Policy.md + `RedisKey` 유틸 |

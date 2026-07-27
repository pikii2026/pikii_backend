# Day 2 — Recruit 상세조회 / 작성 / AI초안 (3-1, 3-2, 3-3)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 3-1 공고 상세 조회 | `GET /recruits/{recruitId}` | 로그인/비로그인 모두 200, `isScrapped`는 비로그인 시 항상 false |
| 3-2 공고 작성 | `POST /recruits` | 201 + `recruitId` 반환, Category/Topic 매핑까지 저장 |
| 3-3 AI 공고 초안 생성 | `POST /recruits/ai-draft` | AI 서버 미연동 상태면 목(mock) 응답으로 우선 200 반환 |

**DoD**: 3-2로 만든 공고를 3-1로 조회했을 때 카테고리/주제가 정확히 포함되어 나온다.

---

## 사전 확인 사항

- **의존 관계**: `Recruit` 생성 시 `Member`(작성자) 참조가 필요합니다 — 로그인 필수(`@AuthenticationPrincipal Long memberId`). 회원가입/로그인은 이미 완료되어 있으니 문제 없음.
- Day1에서 만든 Category/Topic 조회 API(5-1, 5-2)가 프론트에서 이 작성 폼의 드롭다운으로 쓰입니다. 순서상 자연스럽게 이어집니다.
- **다른 트랙과의 결합 지점 없음.** 인원2는 아직 이 도메인을 안 건드립니다.
- AI 연동(3-3)은 실제 AI API 키/설정이 없을 가능성이 높습니다. **일단 목업으로 구현**하고, 실제 연동은 시간 남으면 나중에 붙이는 걸 추천합니다 (12일 스코프엔 AI 품질이 중요하지 않음).

---

## 구현 순서

### 3-2 공고 작성 (먼저 구현 — 3-1이 이걸 전제로 함)

**요청**
```json
POST /recruits
{
  "title":"제일기획 공모전",
  "maxMembers":4,
  "onCampus":true,
  "categoryIds":[1,2],
  "topicIds":[1,4],
  "startDate":"2026-07-10",
  "endDate":"2026-08-10",
  "simpleDesc":"간단 소개",
  "content":"상세 내용"
}
```
> 스펙 문서 필드명은 `maxMembers`이지만 엔티티 필드는 `targetCount`입니다. DTO에서 `maxMembers`로 받아 엔티티 생성 시 `targetCount`로 매핑하세요.

**응답**: `201 Created` + `{ "recruitId": 1 }`

**검증 규칙**: 제목 2~20자, 모집인원 1~7, 간단소개 50자 이하, 상세내용 필수/1000자 이하, `onCampus` 필수, `categoryIds` 최소 1개.

1. `domain/recruit/dto/RecruitCreateRequest.java` — `@Size`, `@Min`, `@Max`, `@NotNull`, `@NotEmpty(categoryIds)` 등
2. `domain/recruit/dto/RecruitCreateResponse.java` — `record RecruitCreateResponse(Long recruitId) {}`
3. Repository 신규 생성 필요: `RecruitCategoryRepository`, `RecruitTopicRepository` (기존에 없음, `domain/recruit/repository`에 추가). `RecruitCategory`/`RecruitTopic`는 복합키 엔티티라 `JpaRepository<RecruitCategory, RecruitCategory.Pk>` 형태로 선언
4. `domain/recruit/service/RecruitService.java` (신규)
   ```java
   @Transactional
   public RecruitCreateResponse create(Long memberId, RecruitCreateRequest request) {
       Member member = memberRepository.findById(memberId)
               .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
       Recruit recruit = Recruit.builder()
               .member(member).title(request.title()).onCampus(request.onCampus())
               .startDate(request.startDate()).endDate(request.endDate())
               .simpleDesc(request.simpleDesc()).content(request.content())
               .targetCount(request.maxMembers())
               .build();
       recruitRepository.save(recruit);
       request.categoryIds().forEach(cid -> recruitCategoryRepository.save(new RecruitCategory(recruit.getId(), cid)));
       request.topicIds().forEach(tid -> recruitTopicRepository.save(new RecruitTopic(recruit.getId(), tid)));
       return new RecruitCreateResponse(recruit.getId());
   }
   ```
5. `domain/recruit/controller/RecruitController.java` (신규) — `POST /recruits`

### 3-1 공고 상세 조회

**응답 예시**는 API_SPEC 3-1 참고 (`authorNickname`, `authorEXP`, `category`, `topics`, `availableSlots`, `isScrapped` 포함).

1. `RecruitDetailResponse` DTO — `availableSlots`는 `recruit.getAvailableSlots()` 그대로 사용
2. Service: `getDetail(Long recruitId, Long memberId)` — `memberId`는 nullable(비로그인 시 null). `memberId != null`이면 `recruitScrapRepository.existsById(new RecruitScrap.Pk(memberId, recruitId))`로 `isScrapped` 계산, null이면 무조건 false
3. Controller: `GET /recruits/{recruitId}` — **인증 불필요 API이므로 `@AuthenticationPrincipal Long memberId`가 null로 들어올 수 있게 처리**해야 합니다. (JwtAuthenticationFilter가 토큰 없어도 필터를 통과시키는지 확인 필요 — 토큰 없이 요청 시 401이 나면 안 됨)
4. `SecurityConfig`에 이미 `GET /recruits/*`가 permitAll로 등록돼 있는지 확인 (1-1 작업 때 이미 넣어둔 걸로 기억하지만 재확인)

### 3-3 AI 공고 초안 생성 (목업)

```java
@PostMapping("/recruits/ai-draft")
public ResponseEntity<ApiResponse<AiDraftResponse>> generateDraft(@RequestBody AiDraftRequest request) {
    // TODO: 실제 AI API 연동 전까지는 입력값을 살짝 가공해 돌려주는 목업으로 구현
    var response = new AiDraftResponse(
        "AI가 다듬은: " + request.simpleDesc(),
        "AI가 다듬은: " + request.content()
    );
    return ResponseEntity.ok(ApiResponse.of(response));
}
```
실패 케이스(`500 AI_GENERATION_FAILED`)는 실제 AI 연동 전까지는 테스트 불가하니 스킵해도 됩니다.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 존재하지 않는 recruitId 조회 | 404 `RECRUIT_NOT_FOUND` |
| 작성 시 검증 실패(제목 길이 등) | 400 `VALIDATION_FAILED` |

---

## 테스트 체크리스트

- [ ] 공고 작성 → 201 + recruitId 반환
- [ ] 방금 만든 공고를 상세조회 → 카테고리/주제/제목 등 정확히 일치
- [ ] 비로그인 상태로 상세조회 → 401 아니라 200, `isScrapped: false`
- [ ] 존재하지 않는 recruitId 조회 → 404
- [ ] `categoryIds` 빈 배열로 작성 시도 → 400
- [ ] 제목 21자 이상으로 작성 시도 → 400

---

## 커밋/PR 가이드

- 브랜치: `feat/recruit-create`
- 커밋: `feat: 공고 작성 API 구현 (3-2)` → `feat: 공고 상세 조회 API 구현 (3-1)` → `feat: AI 공고 초안 생성 API 목업 구현 (3-3)`
- PR: 3개 묶어서 하나로 올려도 되고(작은 도메인 단위), 나눠도 무방

---

## 막힐 수 있는 포인트

- **`@AuthenticationPrincipal Long memberId`가 비로그인 시 어떻게 처리되는지** — `JwtAuthenticationFilter`를 열어서, 토큰이 아예 없을 때 `SecurityContext`에 뭘 넣는지 확인하세요. null을 넣도록 안 돼있으면 3-1(비로그인 조회)에서 막힐 수 있습니다.
- **복합키 엔티티 저장**: `RecruitCategory`, `RecruitTopic`처럼 `@IdClass`를 쓰는 엔티티는 `save()` 호출 시 이미 존재하는 PK면 update로 동작합니다(insert or update). 중복 저장 방지 로직은 딱히 필요 없지만, 헷갈리면 `RecruitScrap.java`(이미 구현된 유사 패턴)를 참고하세요.

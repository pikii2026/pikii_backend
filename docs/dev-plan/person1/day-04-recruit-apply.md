# Day 4 — 지원 AI초안 / 지원하기 / 지원취소 (3-10, 3-11, 3-13)

## ⚠️ 오늘은 전체 스케줄의 최우선 순위입니다

**인원2가 Day1(오늘도 같은 날짜)에 Project 생성(6-1) 로직을 짜기 시작하는데, 그 로직은 "ACCEPTED 상태의 Apply가 존재"해야 동작을 테스트할 수 있습니다.** 인원2는 실제로는 MySQL에 직접 테스트 데이터를 심어서 인원1의 API 완성을 기다리지 않고 진행하지만(Day11 가이드 참고), **가능하면 오늘 중 3-11(지원하기)까지는 끝내두는 게 전체 일정에 안전합니다.**

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 3-10 AI 지원서 초안 생성 | `POST /recruits/{recruitId}/applies/ai-draft` | 목업으로 200 |
| 3-11 공고 지원하기 | `POST /recruits/{recruitId}/applies` | 204, 키워드 최대 5개, 중복지원 방지 |
| 3-13 지원 취소 | `DELETE /applies/{applyId}` | 204, WAITING 상태만 취소 가능 |

---

## 사전 확인 사항

- `Apply` 엔티티는 이미 `accept()`, `reject()`, `isWaiting()` 메서드를 갖고 있습니다 (`domain/apply/entity/Apply.java` 확인).
- `ApplyRepository`는 존재하지만 커스텀 쿼리 메서드(`existsByRecruitIdAndMemberId` 등)는 없을 수 있으니 필요하면 추가하세요.
- **결합 지점**: 이 API로 만들어진 `Apply(WAITING)` → 나중에 Day7(4-8 지원자 수락)에서 `ACCEPTED`로 바뀌고 → 그게 인원2의 Project 생성(6-1)의 전제조건입니다. 체인이 이어진다는 걸 염두에 두고 만드세요.

---

## 구현 순서

### 3-11 공고 지원하기 (먼저 구현 — 오늘의 핵심)

**요청**
```json
POST /recruits/{recruitId}/applies
{ "message":"자기소개 및 지원 동기", "keywordIds":[1,2,3] }
```

1. `domain/apply/dto/ApplyCreateRequest.java` — `message`(최대 300자), `keywordIds`(nullable, 최대 5개 — `@Size(max=5)`)
2. Repository에 추가: `ApplyRepository.existsByRecruitIdAndMemberId(Long recruitId, Long memberId)`
3. `ApplyService.apply(Long memberId, Long recruitId, ApplyCreateRequest request)`
   ```java
   Recruit recruit = recruitRepository.findById(recruitId)
           .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
   if (recruit.getStatus() == RecruitStatus.CLOSED) {
       throw new BusinessException(ErrorCode.RECRUIT_CLOSED);
   }
   if (applyRepository.existsByRecruitIdAndMemberId(recruitId, memberId)) {
       throw new BusinessException(ErrorCode.ALREADY_APPLIED);
   }
   Apply apply = Apply.builder().recruit(recruit).member(member).message(request.message()).build();
   // Apply 생성자에서 status 기본값 WAITING으로 세팅되어 있는지 Apply.java에서 확인
   applyRepository.save(apply);
   if (request.keywordIds() != null) {
       request.keywordIds().forEach(kid -> applyKeywordMapRepository.save(new ApplyKeywordMap(apply.getId(), kid)));
   }
   // TODO: 작성자에게 알림 발송 — Notification 도메인 완성 후(Day9) 연결. 오늘은 주석으로 남겨두고 스킵 가능
   ```
4. `ApplyKeywordMapRepository` 신규 생성 필요 (없으면).
5. Controller: `POST /recruits/{recruitId}/applies` → `204 No Content`

### 3-13 지원 취소

```java
@Transactional
public void cancel(Long memberId, Long applyId) {
    Apply apply = applyRepository.findById(applyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND));
    if (!apply.getMember().getId().equals(memberId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!apply.isWaiting()) {
        throw new BusinessException(ErrorCode.APPLY_NOT_WAITING);
    }
    applyRepository.delete(apply);
}
```
Controller: `DELETE /applies/{applyId}` → `204`

### 3-10 AI 지원서 초안 생성 (목업)

Day2의 3-3과 동일 패턴 — 입력 메시지를 가공해서 돌려주는 목업으로 충분합니다.

```java
@PostMapping("/recruits/{recruitId}/applies/ai-draft")
public ResponseEntity<ApiResponse<AiApplyDraftResponse>> generateDraft(
        @PathVariable Long recruitId, @RequestBody AiApplyDraftRequest request) {
    var response = new AiApplyDraftResponse("[AI 다듬음] " + request.message());
    return ResponseEntity.ok(ApiResponse.of(response));
}
```
> 이 API는 로그인만 되면 되고 프로필(이력서) 없이도 사용 가능합니다(스펙 참고).

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| CLOSED 공고에 지원 시도 | 400 `RECRUIT_CLOSED` |
| 이미 지원한 공고에 재지원 | 409 `ALREADY_APPLIED` |
| 존재하지 않는 지원서 취소 시도 | 404 `APPLY_NOT_FOUND` |
| WAITING 아닌 지원서 취소 시도 | 409 `APPLY_NOT_WAITING` |
| 남의 지원서 취소 시도 | 403 `FORBIDDEN` |
| 키워드 6개 이상 선택 | 400 `VALIDATION_FAILED` |

---

## 테스트 체크리스트

- [ ] OPEN 공고에 지원 → 204, DB에 Apply(WAITING) 생성 확인
- [ ] 같은 공고에 같은 사람이 재지원 → 409
- [ ] CLOSED 공고에 지원 시도 → 400
- [ ] 키워드 6개 선택해서 지원 시도 → 400
- [ ] 방금 지원한 걸 취소 → 204, DB에서 삭제 확인
- [ ] ACCEPTED 상태로 미리 바꿔둔(SQL 직접 UPDATE) 지원서 취소 시도 → 409

---

## 커밋/PR 가이드

- 브랜치: `feat/recruit-apply`
- 커밋: `feat: 공고 지원하기/취소 API 구현 (3-11, 3-13)` → `feat: AI 지원서 초안 생성 API 목업 구현 (3-10)`

---

## 막힐 수 있는 포인트

- **`ApplyKeywordMap` 복합키**: `RecruitCategory`와 동일한 `@IdClass` 패턴일 가능성이 높습니다. 엔티티 파일(`domain/apply/entity/ApplyKeywordMap.java`)을 먼저 열어서 생성자 시그니처를 확인하세요.
- **알림 발송 코드를 지금 넣을지 말지**: Notification 도메인은 Day9에 만듭니다. 오늘은 알림 발송 부분을 주석(`// TODO: Notification 연동`)으로 남겨두고, Day9에 가서 실제로 채워 넣으세요. 억지로 오늘 Notification까지 손대지 마세요.
- **인원2가 이 API를 기다리지 않는다는 점을 인지**: 인원2는 MySQL에 직접 `INSERT INTO apply (..., status) VALUES (..., 'ACCEPTED')`로 테스트 데이터를 심어서 진행합니다. 이 API가 늦어져도 인원2 작업이 막히진 않지만, Day11 통합 테스트 때는 실제 API로 전체 흐름이 이어져야 하니 오늘/내일 중으로는 끝내주세요.

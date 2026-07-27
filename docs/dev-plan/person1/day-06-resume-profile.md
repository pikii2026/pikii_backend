# Day 6 — 이력서(프로필) CRUD (4-1, 4-2, 4-3, 10-1)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 4-1 내 프로필 조회 | `GET /users/me` | 200, 하위 항목(기술스택/자격증/경험/링크) 전부 포함 |
| 4-2 프로필 생성 | `POST /users/create-resume` | 201, 가입이메일 → contactEmail 복사, aboutMe는 AI 목업 생성 |
| 4-3 프로필 수정 | `PATCH /users/me` | 204 |
| 10-1 남의 프로필 조회 | `GET /users/{memberId}` | 4-1과 응답 구조 동일 + 탈퇴회원 처리 |

**DoD**: 생성 → 조회 → 수정 → 재조회 사이클이 하위 항목(기술스택, 자격증, 경험, 링크) 전부 포함해서 한 번에 매끄럽게 돈다.

---

## 사전 확인 사항

- 이 도메인은 **1인당 최대 7개 하위 테이블**(MemberUniv, MemberResume, DetailTopic, MemberTechStack, MemberLicense, DetailExperience, AdditionalLink)을 한 번에 다뤄야 해서 오늘 자체가 CRUD치고는 무겁습니다. 서두르지 말고 4-2(생성)부터 차근차근 가세요.
- Day1에서 만든 Master Data API(기술스택/자격증/링크카테고리/대학교 목록)가 이 화면의 드롭다운 데이터입니다 — 이미 끝나 있어서 오늘은 참조만 하면 됩니다.
- **Repository 신규 생성 필요**: `DetailTopicRepository`, `MemberTechStackRepository`, `MemberLicenseRepository`, `DetailExperienceRepository`, `AdditionalLinkRepository` (현재 `MemberResumeRepository`만 존재).
- `MemberUnivRepository`는 이미 존재합니다.

---

## 구현 순서

### 4-2 프로필 생성 (먼저 구현)

**요청 필드** (`RecruitCreateRequest`와 마찬가지로 API_SPEC 4-2 표 참고): `univId`(필수), `major`(필수, 2~50자), `academicStatus`(필수 Enum), `hope`, `strength`, `topic`(List), `skillTool`(List), `license`(List), `experience`(List), `additionalLink`(List)

1. DTO: `ResumeCreateRequest` — 중첩 리스트는 각각 별도 record로 (`SkillToolItem(String techStackName, int level)` 등). **주의**: 요청은 `techStackName`(이름)으로 오지만 저장은 `techStackId`가 필요합니다 — 이름으로 Master Data를 조회해서 ID를 찾아야 합니다.
2. `ResumeService.create(Long memberId, ResumeCreateRequest request)`:
   ```java
   Member member = memberRepository.findById(memberId).orElseThrow(...);
   Univ univ = univRepository.findById(request.univId())
           .orElseThrow(() -> new BusinessException(ErrorCode.UNIV_NOT_FOUND));
   memberUnivRepository.save(new MemberUniv(member, univ, request.major(), request.academicStatus()));

   String aboutMe = generateAboutMe(request); // 아래 참고 — AI 목업
   memberResumeRepository.save(new MemberResume(member, member.getEmail(), request.hope(), request.strength(), aboutMe));

   request.topic().forEach(tid -> detailTopicRepository.save(new DetailTopic(memberId, tid)));
   request.skillTool().forEach(s -> {
       TechStack ts = techStackRepository.findByName(s.techStackName())
               .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));
       memberTechStackRepository.save(new MemberTechStack(memberId, ts.getId(), s.level()));
   });
   // license, additionalLink도 동일 패턴 (이름 → ID 조회 후 저장)
   request.experience().forEach(e -> detailExperienceRepository.save(
           new DetailExperience(member, e.title(), e.organization(), e.description(), e.startDate(), e.endDate())));
   ```
3. **`contactEmail`은 요청에서 받지 않고 `member.getEmail()`을 그대로 사용** — 스펙에 명시된 부분이니 실수하지 마세요.
4. **AboutMe 생성(목업)**: 실제 AI 미연동이면 아래처럼 입력값을 조합한 문자열로 대체
   ```java
   private String generateAboutMe(ResumeCreateRequest request) {
       return "%s를 희망하며, %s. (AI 자동 생성 - 목업)".formatted(request.hope(), request.strength());
   }
   ```
5. Controller: `POST /users/create-resume` → `201 Created` + `{"message":"Resume Created"}`

### 4-1 / 10-1 프로필 조회 (거의 동일 로직 — 공유)

```java
public ResumeDetailResponse getMyResume(Long memberId) {
    return buildResponse(memberId);
}

public ResumeDetailResponse getMemberResume(Long targetMemberId) {
    // 10-1: 대상 회원이 없거나(탈퇴) 프로필이 없으면 RESUME_NOT_FOUND
    return buildResponse(targetMemberId);
}

private ResumeDetailResponse buildResponse(Long memberId) {
    MemberResume resume = memberResumeRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    MemberUniv univ = memberUnivRepository.findById(memberId).orElseThrow(...);
    List<DetailTopic> topics = detailTopicRepository.findByMemberId(memberId);
    List<MemberTechStack> techStacks = memberTechStackRepository.findByMemberId(memberId);
    // ... 나머지 하위 목록 조회 후 DTO 조립
}
```
> **10-1과 4-1은 응답 구조가 거의 동일**(10-1은 `contactEmail`도 그대로 노출 — 스펙상 "이력서는 전체 공개"). 내부적으로 같은 `buildResponse` 메서드를 재사용하는 게 효율적입니다.

Controller:
- `GET /users/me` (본인, `@AuthenticationPrincipal`)
- `GET /users/{memberId}` (남의 프로필, PathVariable)

### 4-3 프로필 수정

Body는 4-2와 동일 + `aboutMe`, `contactEmail` 직접 수정 가능. **AI 재생성 없음** — 사용자가 준 값 그대로 저장.

가장 간단한 구현: 기존 하위 목록을 전부 삭제하고 재생성(4-2와 유사 로직 재사용). MemberResume/MemberUniv는 필드만 갱신.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 존재하지 않는 univId로 생성/수정 | 404 `UNIV_NOT_FOUND` |
| 프로필 없는 회원 조회(본인 4-1, 타인 10-1 모두) | 404 `RESUME_NOT_FOUND` |
| 전공 길이 위반 등 | 400 `VALIDATION_FAILED` |

---

## 테스트 체크리스트

- [ ] 프로필 생성 → 4-1로 조회 시 입력한 모든 하위 항목(기술스택/자격증/경험/링크/주제)이 정확히 나옴
- [ ] `contactEmail`이 요청에 안 보냈는데도 가입 이메일로 자동 채워짐
- [ ] `aboutMe`가 비어있지 않고 자동 생성됨
- [ ] 프로필 수정 후 재조회 시 변경사항 반영
- [ ] 다른 회원 ID로 10-1 조회 → 정상 응답
- [ ] 프로필 없는(생성 안 한) 회원 ID로 10-1 조회 → 404 `RESUME_NOT_FOUND`
- [ ] 존재하지 않는 univId로 생성 시도 → 404

---

## 커밋/PR 가이드

- 브랜치: `feat/resume-profile`
- 커밋: `feat: 프로필(이력서) 생성 API 구현 (4-2)` → `feat: 프로필 조회 API 구현 (4-1, 10-1)` → `feat: 프로필 수정 API 구현 (4-3)`

---

## 막힐 수 있는 포인트

- **기술스택/자격증/링크를 "이름"으로 받아서 "ID"로 변환하는 부분**이 오늘 제일 헷갈릴 수 있습니다. 스펙 예시를 다시 보면 요청 바디에 `techStackName`(문자열)로 오고 DB에는 `techStackId`로 저장해야 해서, 중간에 Master Data 조회가 한 번씩 끼어듭니다.
- **탈퇴 회원 조회 처리(10-1)**: `Member`가 Hard Delete되면 `memberRepository.findById()`부터 실패합니다. 이 경우도 `RESUME_NOT_FOUND`로 통일해서 응답하세요(회원이 없는 것도, 프로필이 없는 것도 프론트 입장에선 똑같이 "알 수 없음" 처리).
- **수정(4-3) 시 하위 목록 갱신 전략**: "삭제 후 재생성"이 제일 쉽지만, 항목이 많아지면 쿼리가 늘어납니다. 오늘은 정확성 우선으로 이 방식으로 가고, 최적화는 나중 과제로 미뤄두세요.

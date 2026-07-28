# Day 5 — 공고 수정/스크랩 마무리 + 메인 검색 (3-12, 3-14~3-16, 2-1)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 3-12 공고 수정 | `PATCH /recruits/{recruitId}` | 204, 카테고리/주제 매핑 갱신 |
| 3-14 공고 스크랩 | `POST /recruits/{recruitId}/scrap` | 201 |
| 3-15 스크랩 취소 | `DELETE /recruits/{recruitId}/scrap` | 204 |
| 3-16 스크랩 목록 | `GET /users/me/scraps` | Pagination 정상 동작 |
| 2-1 공고 검색/목록 | `GET /recruits` | 필터 조합(AND/OR) 정확히 동작 |

**DoD**: 오늘로 Recruit 도메인(3장 전체)이 완결됩니다. 2-1까지 끝나면 메인 화면이 통째로 동작합니다.

---

## 사전 확인 사항

- `RecruitScrap` 엔티티는 `@IdClass`(복합키: memberId+recruitId) 구조로 이미 있습니다. `RecruitScrapRepository`도 이미 존재.
- 오늘의 핵심 난이도는 **2-1의 동적 검색 쿼리**입니다. QueryDSL은 설정되어 있지 않으므로 **`JpaSpecificationExecutor`** 방식을 사용하세요.

---

## 구현 순서

### 3-12 공고 수정

Body는 3-2(작성)와 동일. 작성자 확인 → 필드 수정 → 카테고리/주제 매핑 삭제 후 재생성(가장 간단한 방식).

```java
@Transactional
public void update(Long memberId, Long recruitId, RecruitUpdateRequest request) {
    Recruit recruit = getOwnedRecruit(memberId, recruitId); // Day3에서 만든 private 메서드 재사용
    recruit.update(request.title(), request.onCampus(), request.startDate(), request.endDate(),
                   request.simpleDesc(), request.content(), request.maxMembers());
    // Recruit 엔티티에 update() 메서드가 없으면 오늘 추가하세요 (Setter 금지 규칙 — 의미있는 메서드로)
    recruitCategoryRepository.deleteAllByRecruitId(recruitId);
    recruitTopicRepository.deleteAllByRecruitId(recruitId);
    request.categoryIds().forEach(cid -> recruitCategoryRepository.save(new RecruitCategory(recruitId, cid)));
    request.topicIds().forEach(tid -> recruitTopicRepository.save(new RecruitTopic(recruitId, tid)));
}
```
> `Recruit.java`에 `update(...)` 메서드가 없다면 오늘 추가해야 합니다. Setter는 금지 규칙이니 의미 있는 이름의 메서드로 만드세요.

### 3-14, 3-15, 3-16 — 스크랩 3종

```java
// 3-14 스크랩
@Transactional
public void scrap(Long memberId, Long recruitId) {
    if (!recruitRepository.existsById(recruitId)) throw new BusinessException(ErrorCode.RECRUIT_NOT_FOUND);
    var pk = new RecruitScrap.Pk(memberId, recruitId);
    if (recruitScrapRepository.existsById(pk)) throw new BusinessException(ErrorCode.ALREADY_SCRAPPED);
    recruitScrapRepository.save(new RecruitScrap(memberId, recruitId));
}

// 3-15 스크랩 취소
@Transactional
public void unscrap(Long memberId, Long recruitId) {
    var pk = new RecruitScrap.Pk(memberId, recruitId);
    if (!recruitScrapRepository.existsById(pk)) throw new BusinessException(ErrorCode.SCRAP_NOT_FOUND);
    recruitScrapRepository.deleteById(pk);
}
```
3-16(목록)은 `recruitScrapRepository.findByMemberId(memberId, Pageable)` → 각 항목마다 Recruit 정보 join해서 DTO 변환. **삭제된 공고(`deletedAt` not null)는 제외**하는 걸 잊지 마세요.

### 2-1 공고 검색/목록 — 오늘의 핵심

**필터 조합 규칙**: `키워드 AND (카테고리1 OR 카테고리2...) AND (주제1 OR 주제2...) AND 교내/교외`

`RecruitRepository`에 `JpaSpecificationExecutor<Recruit>` 상속 추가:
```java
public interface RecruitRepository extends JpaRepository<Recruit, Long>, JpaSpecificationExecutor<Recruit> {
}
```

Specification 조립 (`RecruitSpecification.java` 신규 생성):
```java
public class RecruitSpecification {
    public static Specification<Recruit> search(String keyword, List<Long> categoryIds,
                                                  List<Long> topicIds, Boolean onCampus) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (keyword != null) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            if (onCampus != null) {
                predicates.add(cb.equal(root.get("onCampus"), onCampus));
            }
            if (categoryIds != null && !categoryIds.isEmpty()) {
                Join<Object, Object> categoryJoin = root.join("categories"); // 매핑 방식에 따라 서브쿼리로 대체 가능
                predicates.add(categoryJoin.get("categoryId").in(categoryIds));
            }
            // topicIds도 동일 패턴
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```
> `Recruit` 엔티티에 `categories`/`topics` 연관관계가 매핑되어 있지 않으면(현재는 `RecruitCategory`가 독립 엔티티라 매핑이 없을 가능성이 큼), **Join 대신 서브쿼리**(`recruitId IN (SELECT recruitId FROM RecruitCategory WHERE categoryId IN :categoryIds)`)로 구현하는 게 더 간단합니다. 이 부분이 오늘 가장 시간 걸릴 수 있는 지점이니, 막히면 Repository에 `@Query` JPQL로 직접 짜는 것도 대안입니다.

Controller: `GET /recruits?keyword=&onCampus=&categoryIds=&topicIds=&page=&size=&sort=` — `Pageable` 파라미터로 자동 바인딩.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 남의 공고 수정 시도 | 403 `FORBIDDEN` |
| 이미 스크랩한 공고 재스크랩 | 409 `ALREADY_SCRAPPED` |
| 스크랩 안 한 공고 취소 시도 | 404 `SCRAP_NOT_FOUND` |
| 검색어 2자 미만 | 400 `VALIDATION_FAILED` |

---

## 테스트 체크리스트

- [ ] 공고 수정 후 상세조회 시 변경 내용 + 새 카테고리/주제 반영 확인
- [ ] 스크랩 → 스크랩 목록에 나옴 → 취소 → 목록에서 사라짐
- [ ] 이미 스크랩한 공고 재스크랩 시도 → 409
- [ ] `keyword=공모전` 검색 시 제목에 "공모전" 포함된 것만
- [ ] `categoryIds=1&categoryIds=2` 두 개 선택 시 둘 중 하나라도 속한 공고 전부 나오는지(OR)
- [ ] `categoryIds=1&topicIds=1` 동시 지정 시 두 조건 모두 만족하는 것만(AND) — 이게 제일 헷갈리는 부분이니 꼭 직접 데이터 3~4개 만들어서 검증
- [ ] `onCampus=true` 필터 시 교내 공고만

---

## 커밋/PR 가이드

- 브랜치: `feat/recruit-finalize`
- 커밋: `feat: 공고 수정 API 구현 (3-12)` → `feat: 공고 스크랩 CRUD 구현 (3-14~3-16)` → `feat: 공고 검색/필터 API 구현 (2-1)`
- PR 제목: `feat: Recruit 도메인 마무리 + 메인 검색 (API_SPEC 2-1, 3-12, 3-14~3-16)` — 이걸로 Recruit 전체 PR을 마무리 짓는 느낌으로 올리면 좋습니다

---

## 막힐 수 있는 포인트

- **2-1의 AND/OR 조합이 오늘 가장 까다로운 부분**입니다. 처음부터 완벽하게 짜려 하지 말고, keyword 필터만 먼저 동작시킨 뒤 하나씩(onCampus → category → topic) 추가하는 식으로 점진적으로 접근하세요.
- **온캠퍼스 공고 노출 규칙** (문서 참고: `onCampus=true`인 공고는 작성자와 같은 대학교 소속에게만 노출)까지 오늘 다 구현하려면 시간이 부족할 수 있습니다. **1차로는 필터 파라미터만 정상 동작시키고, "같은 학교만 노출" 로직은 시간이 남으면 추가**하는 걸 추천합니다(스코프 우선순위 조정).

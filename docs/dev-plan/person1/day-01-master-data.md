# Day 1 — Master Data (5-1 ~ 5-8)

## 오늘의 목표

| 엔드포인트 | 설명 |
|---|---|
| 5-1 `GET /categories` | 카테고리 조회 |
| 5-2 `GET /topics` | 주제 조회 |
| 5-3 `GET /tech-stacks` | 기술 스택 조회 |
| 5-4 `GET /licenses` | 자격증 조회 |
| 5-5 `GET /link-categories` | 링크 카테고리 조회 |
| 5-7 `GET /apply-keywords` | 지원 키워드 조회 (Nested) |
| 5-8 `GET /universities` | 대학교 목록 조회 (검색 지원) |

> 5-6(피드백 키워드, `GET /keywords`)은 인원2가 Feedback 작업(Day10) 때 같이 가져갑니다. 오늘은 건드리지 않아도 됩니다.

**DoD(Definition of Done)**: 위 7개 엔드포인트가 Swagger에서 200 OK로 응답하고, 각 테이블에 시드 데이터가 없어도(빈 배열이어도) 에러 없이 동작한다.

---

## 사전 확인 사항

- 전부 **인증 불필요**, **단순 조회**라서 다른 트랙과의 결합 지점이 없습니다. 오늘은 완전히 독립적으로 진행 가능합니다.
- 단, `Category`(5-1)와 `Topic`(5-2)의 Repository는 **이미 존재**합니다(`domain/recruit/repository`). 나머지는 Repository부터 새로 만들어야 합니다.
- `Univ` Repository도 이미 존재합니다(`domain/member/repository/UnivRepository.java`).
- **없는 것들**: `TechStackRepository`, `LicenseRepository`, `LinkCategoryRepository`, `ApplyKeywordRepository`, `ApplyKeywordCategoryRepository` — 오늘 새로 만들어야 합니다.

---

## 구현 순서

이 도메인은 워낙 단순해서(전부 `findAll()` 수준) DTO → Repository → Service → Controller를 8개 API에 대해 반복하는 하루입니다. 패턴이 다 똑같으니 하나 완성하면 나머지는 복붙 후 이름만 바꾸면 됩니다.

### 공통 패턴 (5-1 카테고리 조회 기준 예시)

```java
// domain/recruit/dto/CategoryResponse.java
public record CategoryResponse(Long categoryId, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}

// domain/recruit/service/MasterDataService.java (또는 각 도메인별로 분리해도 무방)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}

// domain/recruit/controller/CategoryController.java
@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.of(categoryService.getCategories()));
    }
}
```

### 도메인별 구현 목록

| API | 파일 위치 (패키지) | 비고 |
|---|---|---|
| 5-1 카테고리 | `domain/recruit/{dto,service,controller}` | Repository 이미 있음 |
| 5-2 주제 | `domain/recruit/{dto,service,controller}` | Repository 이미 있음 |
| 5-3 기술스택 | `domain/resume/{dto,service,controller}` | `TechStackRepository` 신규 생성. 응답에 `type`(SKILL/TOOL) 포함 |
| 5-4 자격증 | `domain/resume/{dto,service,controller}` | `LicenseRepository` 신규 생성 |
| 5-5 링크카테고리 | `domain/resume/{dto,service,controller}` | `LinkCategoryRepository` 신규 생성. 응답에 `picUrl` 포함 |
| 5-7 지원키워드 | `domain/apply/{dto,service,controller}` | `ApplyKeywordRepository`, `ApplyKeywordCategoryRepository` 신규 생성. **Nested 구조** 주의 (아래 참고) |
| 5-8 대학교 | `domain/member/{dto,service,controller}` | Repository 이미 있음. `keyword` 쿼리파라미터로 부분 검색 필요 → `UnivRepository`에 `findByNameContaining(String keyword)` 추가 |

### 5-7 지원 키워드 — Nested 구조 주의

응답이 카테고리 안에 키워드 리스트가 중첩된 구조입니다.

```json
[
  { "categoryId":1, "category":"실행력 / 책임감", "keywords":[{"keywordId":1,"content":"..."}] }
]
```

구현 순서:
1. `ApplyKeywordCategoryRepository.findAll()`로 카테고리 조회
2. 각 카테고리마다 `ApplyKeywordRepository.findByCategoryId(Long categoryId)` 호출 (또는 `ApplyKeywordRepository.findAll()` 한 번에 가져와서 `Collectors.groupingBy(ApplyKeyword::getCategoryId)`로 메모리에서 묶기 — 데이터가 적으므로 이 방식이 쿼리 수를 줄여서 더 낫습니다)
3. DTO 2단 구조: `ApplyKeywordCategoryResponse(categoryId, category, List<KeywordItem> keywords)` + 내부 `record KeywordItem(Long keywordId, String content)`

---

## 테스트 체크리스트

- [ ] 8개 API 전부 Swagger에서 200 OK
- [ ] 시드 데이터가 비어있는 테이블은 빈 배열 `[]` 반환 (에러 아님)
- [ ] 5-7 지원키워드: 응답이 실제로 카테고리→키워드 2단 중첩 구조로 나오는지 확인
- [ ] 5-8 대학교: `?keyword=명지`처럼 부분 검색어를 넣었을 때 필터링되는지, 파라미터 없을 때 전체 목록이 나오는지 둘 다 확인
- [ ] 인증 없이(Authorization 헤더 없이) 호출해도 401이 안 뜨는지 확인 — `SecurityConfig`의 `permitAll()` 목록에 이 8개 경로를 추가했는지 체크

---

## 커밋/PR 가이드

- 브랜치: `feat/master-data` (main에서 분기)
- 커밋 단위 추천: 도메인 패키지 단위로 3~4개 정도로 나누는 게 적당합니다 (8개를 하나하나 쪼개면 너무 잘게 쪼개짐)
  1. `feat: 카테고리/주제 조회 API 구현 (5-1, 5-2)`
  2. `feat: 기술스택/자격증/링크카테고리 조회 API 구현 (5-3~5-5)`
  3. `feat: 지원 키워드 조회 API 구현 (5-7)`
  4. `feat: 대학교 목록 검색 API 구현 (5-8)`
- PR 제목: `feat: Master Data 조회 API 구현 (API_SPEC 5.)`

---

## 막힐 수 있는 포인트

- **`SecurityConfig` permitAll 깜빡함**: 이 8개는 전부 인증 불필요인데, `SecurityConfig.java`의 `.requestMatchers(...).permitAll()` 목록에 새 경로를 안 넣으면 401이 뜹니다. `/categories`, `/topics`, `/tech-stacks`, `/licenses`, `/link-categories`, `/apply-keywords`, `/universities`를 GET으로 추가하세요.
- **`ApplyKeywordCategoryRepository` 신규 생성 시 패키지 위치**: `ApplyKeywordCategory`, `ApplyKeyword` 엔티티는 `domain/apply/entity`에 있으니 Repository도 `domain/apply/repository`에 만드세요.
- **N+1 쿼리**: 5-7에서 카테고리마다 매번 DB 쿼리를 날리면 카테고리 수만큼 쿼리가 나갑니다. 데이터가 적어서 성능 문제는 없겠지만, 그래도 `findAll()` 한 번 + 메모리 그룹핑 방식을 권장합니다.
- **`SecurityConfig`는 인원2도 건드릴 가능성이 있는 공통 파일**입니다. 수정 전에 최신 main을 pull하고, PR 올리기 전에 인원2에게 한마디 해두세요.

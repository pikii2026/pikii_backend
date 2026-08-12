

# API Specification

Version : v1.0

---

# 0. Common

## 0.1 Base URL

### Local

```
http://localhost:8080/api/v1
```

---

## 0.2 Headers

### Request Header

| Header        | Description          |
|:------------- |:-------------------- |
| Content-Type  | application/json     |
| Authorization | Bearer {AccessToken} |

Authorization Header는 인증이 필요한 API에서만 사용한다.

---

## 0.3 Time Format

모든 날짜와 시간은 ISO-8601 형식을 사용한다.

```
2026-07-06T19:30:00+09:00
```

Timezone

```
KST (UTC+9)
```

---

## 0.4 Null Policy

값이 NULL인 필드는 응답에서 제외한다.

Spring Boot

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
```

---

## 0.5 Success Response

### 200 OK

```json
{
    "data": {},
    "timestamp":"2026-07-06T19:30:00+09:00"
}
```

---

### 201 Created

```json
{
    "data":{},
    "timestamp":"2026-07-06T19:30:00+09:00"
}
```

---

### 204 No Content

Response Body 없음

---

## 0.6 Error Response

```json
{
    "error":{
        "code":"ERROR_CODE",
        "message":"Error Message"
    },
    "timestamp":"2026-07-06T19:30:00+09:00"
}
```

Error Code는

> ERROR_CODE.md 참고

---

## 0.7 공통 Validation 규칙

| 항목      | 규칙                                  |
|:------- |:----------------------------------- |
| 이메일     | 일반 이메일 형식(RFC 5322) 검증. **도메인 제한 없음** |
| 비밀번호    | 8자 이상 ~ 20자 이하, 영문 대문자·소문자·숫자 각 1자 이상 포함 |
| 닉네임     | 1자 이상 ~ 10자 이하                      |
| 전공      | 2자 이상 ~ 50자 이하                      |
| 희망 진로   | 100자 이하                             |
| 장점      | 300자 이하                             |
| 검색 키워드  | 2자 이상 ~ 30자 이하                      |
| 공고 제목   | 2자 이상 ~ 20자 이하                      |
| 모집 인원   | 1명 이상 ~ 7명 이하                       |
| 간단 소개   | 50자 이하                              |
| 상세 내용   | **필수**, 1,000자 이하                    |
| 댓글      | 2자 이상 ~ 100자 이하                     |
| 지원 메시지  | 300자 이하                             |
| 상호평가 주관식 | 30자 이상 ~ 500자 이하 (장점 / 개선점 각각)      |

검증 실패 시 `VALIDATION_FAILED`를 반환한다.

### 이메일 정책

가입 이메일은 **특정 도메인으로 제한하지 않는다.**

- 대학 이메일(`@*.ac.kr`)뿐 아니라 일반 이메일(Gmail, Naver 등) 모두 허용한다.
- 이메일 형식(RFC 5322)만 검증하며, 실제 사용 가능 여부는 **인증 코드 발송/확인**으로 확인한다.
- 학교 정보는 이메일 도메인이 아니라 프로필의 `univ`(학교명) 값으로 관리한다.
- 따라서 학교 이메일 여부로 재학생을 판별하지 않으며, 학적 상태는 `academicStatus`로만 관리한다.

---

## 0.8 Pagination

목록 조회 API는 모두 Pagination을 적용한다.

Query Parameter

| Name | Default        |
|:---- |:-------------- |
| page | 0              |
| size | 10             |
| sort | createdAt,desc |

---

Pagination Response

```json
{
    "data":{
        "content":[

        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":0,
            "totalPages":0,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T19:30:00+09:00"
}
```

---

## 0.9 Authentication

JWT 인증을 사용한다.

```
Access Token

↓

30분

↓

Refresh Token

↓

Redis

↓

RTR
```

Redis 정책은

> REDIS_POLICY.md 참고

---

# 1. Authentication (Auth)

회원가입, 로그인, JWT 인증 및 계정 관리를 위한 API이다.

---

# 1-1 이메일 인증번호 발송

## Endpoint

```http
POST /auth/email/send
```

---

## Authentication

인증 불필요

---

## Request Body

```json
{
    "email": "example@email.com",
    "type": "SIGNUP"
}
```

### Field

| Name  | Type   | Required | Description                    |
|:----- |:------ |:-------- |:------------------------------ |
| email | String | O        | 이메일 (도메인 제한 없음)                |
| type  | Enum   | O        | SIGNUP / PW_RESET / WITHDRAWAL |

---

## Response

### 204 No Content

Response Body 없음

---

## Business Logic

1. 이메일 형식 검증
2. 요청 목적(type)에 따라 가입 여부 확인
3. 6자리 숫자 인증코드 생성
4. Redis에 인증코드 저장
5. 이메일 발송
6. 동일 이메일/IP 요청 제한 적용

Redis 저장

Key

```text
auth:code:{Purpose}:{Email}
```

TTL

```text
180초
```

---

## Error

| HTTP | Error Code           |
|:---- |:-------------------- |
| 409  | EMAIL_ALREADY_EXISTS |
| 404  | USER_NOT_FOUND       |
| 429  | TOO_MANY_REQUESTS    |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md
- ENUM.md

---

# 1-2 이메일 인증번호 확인

## Endpoint

```http
POST /auth/email/verify
```

---

## Authentication

인증 불필요

---

## Request Body

```json
{
    "email":"example@email.com",
    "code":"123456",
    "type":"SIGNUP"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "isVerified":true,
        "emailVerificationToken":"uuid-string"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Redis에서 인증번호 조회
2. 입력 코드 비교
3. 인증 성공 시 인증코드 삭제
4. Verification Token 생성
5. Redis 저장
6. Token 반환

Redis Key

```text
auth:verify:{UUID}
```

TTL

```text
900초
```

---

## Error

| HTTP | Error Code                |
|:---- |:------------------------- |
| 400  | INVALID_VERIFICATION_CODE |
| 404  | VERIFICATION_NOT_FOUND    |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-3 닉네임 중복 확인

## Endpoint

```http
GET /auth/nickname/check
```

---

## Authentication

인증 불필요

---

## Query Parameter

| Name     | Required | Description |
|:-------- |:-------- |:----------- |
| nickname | O        | 검사할 닉네임     |

---

## Response

### 200 OK

```json
{
    "data":{
        "isAvailable":true,
        "nicknameVerificationToken":"uuid-string"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 닉네임 길이 검증
2. 중복 여부 확인
3. Verification Token 생성
4. Redis 저장
5. Token 반환

---

## Error

| HTTP | Error Code              |
|:---- |:----------------------- |
| 400  | VALIDATION_FAILED       |
| 409  | NICKNAME_ALREADY_EXISTS |

---

# 1-4 회원가입

## Endpoint

```http
POST /auth/signup
```

---

## Authentication

인증 불필요

---

## Request Body

```json
{
    "nickname":"pickii",
    "email":"example@email.com",
    "password":"Password",
    "passwordConfirm":"Password",
    "emailVerificationToken":"uuid",
    "nicknameVerificationToken":"uuid",
    "terms":{
        "ageAgreed":true,
        "termsAgreed":true,
        "privacyAgreed":true,
        "profileShareAgreed":true,
        "dataCollectionAgreed":true,
        "pushNotiAgreed":false
    }
}
```

---

## Response

### 201 Created

```json
{
    "data":{
        "memberId":1,
        "email":"example@email.com",
        "nickname":"pickii"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 입력값 검증
2. 비밀번호 일치 여부 확인
3. BCrypt 암호화
4. Email Verification Token 검증
5. Nickname Verification Token 검증
6. 필수 약관 검증
7. Member 생성
8. NotificationSetting 생성
9. Verification Token 삭제

---

## Error

| HTTP | Error Code                 |
|:---- |:-------------------------- |
| 400  | VALIDATION_FAILED          |
| 400  | PASSWORD_MISMATCH          |
| 400  | REQUIRED_TERMS_NOT_AGREED  |
| 401  | INVALID_VERIFICATION_TOKEN |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md
- ENUM.md

---

# 1-5 로그인

## Endpoint

```http
POST /auth/login
```

---

## Authentication

인증 불필요

---

## Request Body

```json
{
    "email":"example@email.com",
    "password":"Password",
    "autoLogin":true,
    "deviceId":"device-uuid"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "memberId":1,
        "nickname":"pickii",
        "accessToken":"...",
        "refreshToken":"..."
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 이메일 조회
2. BCrypt 비밀번호 검증
3. Access Token 발급
4. Refresh Token 발급
5. LastLoginAt 갱신
6. Refresh Token Redis 저장

Redis Key

```text
auth:refresh:{MemberId}:{DeviceId}
```

TTL

- autoLogin = true → 30일
- autoLogin = false → 1일

동일 DeviceId 로그인 시 기존 Refresh Token을 덮어쓴다.

---

## Error

| HTTP | Error Code          |
|:---- |:------------------- |
| 401  | INVALID_CREDENTIALS |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md
- ENUM.md

---

# 1-6 토큰 갱신 (Silent Refresh)

## Endpoint

```http
POST /auth/token/refresh
```

---

## Authentication

Access Token + Refresh Token 필요

---

## Request Body

```json
{
    "deviceId":"device-uuid",
    "refreshToken":"~~~~"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "accessToken":"~~~~",
        "refreshToken":"~~~~"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. Refresh Token 검증
3. Redis Refresh Token 조회
4. 저장된 Refresh Token과 비교
5. Refresh Token Rotation(RTR) 수행
6. 새로운 Access Token 발급
7. 새로운 Refresh Token 발급
8. Redis Refresh Token 교체
9. lastUsedAt 갱신

Redis Key

```text
auth:refresh:{MemberId}:{DeviceId}
```

---

## Error

| HTTP | Error Code            |
|:---- |:--------------------- |
| 401  | INVALID_REFRESH_TOKEN |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-7 로그아웃

## Endpoint

```http
POST /auth/logout
```

---

## Authentication

Bearer Access Token 필요

---

## Request Body

```json
{
    "deviceId":"device-uuid"
}
```

---

## Response

### 204 No Content

---

## Business Logic

1. Access Token 검증
2. MemberId 추출
3. Redis Refresh Token 삭제
4. Access Token Blacklist 등록
5. 남은 Access Token 만료 시간을 TTL로 사용

Redis 삭제

```text
auth:refresh:{MemberId}:{DeviceId}
```

Redis 저장

```text
auth:blacklist:{AccessToken}
```

---

## Error

| HTTP | Error Code    |
|:---- |:------------- |
| 401  | INVALID_TOKEN |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-8 비밀번호 재설정

## Endpoint

```http
POST /auth/password/reset
```

---

## Authentication

인증 불필요

---

## Request Body

```json
{
    "email":"example@email.com",
    "emailVerificationToken":"uuid",
    "newPassword":"Password",
    "newPasswordConfirm":"Password"
}
```

---

## Response

### 204 No Content

---

## Business Logic

1. Verification Token 검증
2. 비밀번호 일치 여부 확인
3. BCrypt 암호화
4. Member Password 변경
5. Redis Refresh Token 전체 삭제
6. 모든 기기 강제 로그아웃

Redis 삭제

```text
auth:refresh:{MemberId}:*
```

---

## Error

| HTTP | Error Code                 |
|:---- |:-------------------------- |
| 400  | PASSWORD_MISMATCH          |
| 401  | INVALID_VERIFICATION_TOKEN |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-9 회원 탈퇴

## Endpoint

```http
DELETE /auth/withdraw
```

---

## Authentication

Bearer Access Token 필요

---

## Request Body

```json
{
    "password":"Password",
    "emailVerificationToken":"uuid",
    "agreements":{
        "dataDeletionAgreed":true,
        "rejoinPolicyAgreed":true
    }
}
```

---

## Response

### 204 No Content

---

## Business Logic

1. Access Token 검증
2. 비밀번호 검증
3. Verification Token 검증
4. 필수 동의 여부 확인
5. Member 개인정보 삭제(Hard Delete 또는 익명화)
6. 작성 게시글, 댓글, 채팅은 유지
7. 작성자 정보를 "알 수 없음" 사용자로 변경
8. Refresh Token 전체 삭제
9. Access Token Blacklist 등록

Redis 삭제

```text
auth:refresh:{MemberId}:*
```

Redis 저장

```text
auth:blacklist:{AccessToken}
```

---

## Error

| HTTP | Error Code                 |
|:---- |:-------------------------- |
| 400  | REQUIRED_TERMS_NOT_AGREED  |
| 401  | INVALID_CREDENTIALS        |
| 401  | INVALID_VERIFICATION_TOKEN |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-10 소셜 로그인

## 정책

- 현재 지원 Provider는 **KAKAO만**이다. (GOOGLE / NAVER는 추후 확장)
- **소셜로 신규 가입할 수 없다.** 소셜은 가입 수단이 아니라 로그인 편의 기능이다.
- 이메일+비밀번호로 가입한 뒤 프로필에서 카카오를 연동해야 소셜 로그인을 사용할 수 있다.
- 연동되지 않은 소셜로 로그인 시도 시 `NOT_LINKED_ACCOUNT`를 반환하며, 클라이언트는
  **"카카오 로그인을 사용하려면 로그인 후 프로필에서 카카오를 먼저 연결해주세요"** 안내를 노출한다.
- 식별 키는 이메일이 아니라 **`providerUserId`**(카카오 고유 id)이다.
- 클라이언트가 보낸 소셜 Access Token은 **반드시 서버가 카카오 API로 재검증**한다.
- 소셜 토큰은 **저장하지 않는다.** 검증 후 즉시 폐기하고, 이후 인증은 우리 JWT로만 수행한다.

```
[앱] 카카오 SDK 로그인 → Access Token 획득

      ↓ POST /auth/social/login

[서버] 카카오 API로 토큰 검증 → providerUserId 획득

      ↓

[서버] SocialAccount(KAKAO, providerUserId) 조회

      ├─ 있음 → 해당 Member로 우리 JWT 발급
      └─ 없음 → 404 NOT_LINKED_ACCOUNT
```

---

## Endpoint

```http
POST /auth/social/{provider}/login
```

---

## Authentication

인증 불필요

---

## Path Variable

| Name     | Description            |
|:-------- |:---------------------- |
| provider | KAKAO / GOOGLE / NAVER |

---

## Request Body

```json
{
    "socialAccessToken":"kakao-access-token",
    "autoLogin":true,
    "deviceId":"device-uuid"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "accessToken":"~~~~",
        "refreshToken":"~~~~"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Provider 서버에 Social Access Token 검증 요청
2. ProviderId 추출
3. SocialAccount 조회
4. 연동된 Member 조회
5. Access Token 발급
6. Refresh Token 발급
7. Redis Refresh Token 저장

---

## Error

| HTTP | Error Code           |
|:---- |:-------------------- |
| 401  | INVALID_SOCIAL_TOKEN |
| 404  | NOT_LINKED_ACCOUNT   |

---

## Related Documents

- REDIS_POLICY.md
- ENUM.md
- ERROR_CODE.md

---

# 1-11 소셜 계정 연동

## Endpoint

```http
POST /auth/social/{provider}/link
```

---

## Authentication

Bearer Access Token 필요

---

## Path Variable

| Name     | Description            |
|:-------- |:---------------------- |
| provider | KAKAO / GOOGLE / NAVER |

---

## Request Body

```json
{
    "providerId":"123456789"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "message":"소셜 계정 연동이 완료되었습니다."
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. MemberId 추출
3. Provider 및 ProviderId 중복 확인
4. SocialAccount 생성
5. Member와 Provider 연동

---

## Error

| HTTP | Error Code     |
|:---- |:-------------- |
| 401  | UNAUTHORIZED   |
| 409  | ALREADY_LINKED |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 1-12 비밀번호 변경 (로그인 상태)

## Endpoint

```http
PATCH /auth/password          # 비밀번호 변경
```

---

## Authentication

Bearer Access Token 필요

---

## 흐름

이미 로그인된 상태이므로 **이메일을 다시 입력받거나 인증코드를 발송하지 않는다.**
본인 확인은 **현재 비밀번호 입력**으로 한다.

```
'비밀번호 변경' 진입

↓

현재 비밀번호 / 새 비밀번호 / 새 비밀번호 확인 입력 → PATCH /auth/password
```

---

## Request Body

```json
{
    "currentPassword":"Password",
    "newPassword":"Password",
    "newPasswordConfirm":"Password"
}
```

### Field

| Name                | Type   | Required | Description   |
|:-------------------- |:------ |:-------- |:------------- |
| currentPassword      | String | O        | 현재 비밀번호       |
| newPassword          | String | O        | 새 비밀번호        |
| newPasswordConfirm   | String | O        | 새 비밀번호 확인     |

> 이메일은 입력받지 않는다. 로그인 사용자의 이메일을 서버가 사용한다.

### Response

204 No Content

### Business Logic

1. Access Token 검증
2. 현재 비밀번호 일치 여부 확인
3. 새 비밀번호 확인 일치 여부 확인
4. BCrypt 암호화 후 저장
5. 모든 기기의 Refresh Token 삭제 → 전체 로그아웃
6. 현재 Access Token Blacklist 등록

---

## 비로그인 재설정(1-8)과의 차이

| 구분     | 비로그인 재설정 (1-8) | 로그인 후 변경 (1-12) |
|:------ |:-------------- |:--------------- |
| 인증     | 불필요            | Access Token 필요 |
| 이메일 입력 | **필요**         | **불필요** (서버가 앎)  |
| 기존 비밀번호 | 불필요            | **필요**          |
| 이메일 인증 | 필요             | 불필요             |

두 API는 분리하여 유지한다.

---

## Error

| HTTP | Error Code         | 설명            |
|:---- |:------------------- |:-------------- |
| 400  | PASSWORD_MISMATCH   | 비밀번호 확인 불일치    |
| 401  | INVALID_TOKEN        | Access Token 오류 |
| 401  | INVALID_CREDENTIALS  | 현재 비밀번호 불일치    |

---

## Related Documents

- REDIS_POLICY.md
- ERROR_CODE.md

---

# 1-13 소셜 계정 연동 조회 / 해제

## Endpoint

```http
GET    /users/me/social-accounts        # 연동 상태 조회
DELETE /auth/social/{provider}/link     # 연동 해제
```

---

## Authentication

Bearer Access Token

---

## 연동 상태 조회 Response

### 200 OK

```json
{
    "data":[
        {
            "provider":"KAKAO",
            "linked":true,
            "linkedAt":"2026-07-01T10:00:00+09:00"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

프로필 화면의 '소셜 계정 연결' 섹션에서 사용한다.

---

## 연동 해제 Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 해당 Provider의 `SocialAccount` row 삭제
3. 카카오 쪽 연결 해제(unlink) API는 호출하지 않는다

> 모든 계정이 이메일+비밀번호를 보유하므로, 연동을 해제해도 로그인 불가 상태가 되지 않는다.
> 잘못된 카카오 계정을 연동한 경우 되돌릴 수 있어야 하므로 해제를 지원한다.

---

## Error

| HTTP | Error Code         | 설명           |
|:---- |:------------------ |:------------ |
| 401  | UNAUTHORIZED       | 로그인되지 않은 사용자 |
| 404  | NOT_LINKED_ACCOUNT | 연동된 계정 없음    |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 2. Main (Home)

메인 화면에서 공고를 검색하고 목록을 조회하는 API이다.

---

# 2-1 공고 검색 및 목록 조회

## Endpoint

```http
GET /recruits
```

---

## Authentication

인증 불필요

---

## Query Parameter

| Name        | Type       | Required | Description    |
|:----------- |:---------- |:-------- |:-------------- |
| keyword     | String     | X        | 검색어(최소 2글자)    |
| onCampus    | Boolean    | X        | true=교내 공고만, false=교외 공고만, 미지정=전체 (교내는 같은 학교 소속만 조회 가능) |
| categoryIds | List<Long> | X        | 카테고리 필터        |
| topicIds    | List<Long> | X        | 주제 필터          |
| page        | Integer    | X        | 기본값 0          |
| size        | Integer    | X        | 기본값 10         |
| sort        | String     | X        | createdAt,desc |


### 필터 결합 규칙

서로 다른 종류의 필터는 **AND(교집합)** 로 결합한다.

```
검색어  AND  (카테고리)  AND  (주제)  AND  (교내/교외)
```

같은 종류의 필터 안에서 여러 항목을 선택한 경우에는 **OR**로 처리한다.

```
(공모전 OR 스터디) AND (기획 OR 디자인) AND 교내
```

> 같은 그룹 내부까지 AND로 처리하면 '공모전이면서 동시에 스터디인 공고'가 되어 결과가 나오지 않는다.
> 카테고리/주제를 단일 선택으로 확정하는 경우 이 규칙은 불필요하다. (미확정)

필터를 하나도 적용하지 않으면 **전체 공고**가 조회된다.

---

### 교내(onCampus) 공고 노출 규칙

`onCampus = true` 공고는 **작성자와 같은 대학교 소속 회원에게만** 노출된다.

```
노출 조건 : 작성자.univId == 조회자.univId
```

- 캠퍼스는 구분하지 않으므로, 같은 대학교면 캠퍼스가 달라도 노출된다.
- 프로필이 없어 학교 정보가 없는 회원과 비회원에게는 교내 공고가 노출되지 않으며, `onCampus=true` 필터를 사용할 수 없다.
- 교외 공고(`onCampus=false`)는 모든 사용자에게 노출된다.

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "recruitId":1,
                "title":"제일기획 공모전 팀원 모집",
                "authorId":10,
                "authorNickname":"픽키",
                "maxMembers":4,
                "availableSlots":2,
                "status":"OPEN",
                "createdAt":"2026-07-01T10:00:00+09:00"
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":12,
            "totalPages":2,
            "hasNext":true
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 검색 조건 검증
2. keyword는 2자 이상 입력
3. category/topic은 AND 조건으로 조회
4. CLOSED 공고도 포함
5. 교내 조건 시, 사용자의 학교 정보가 없는 상태인 경우, 교외만 조회가 가능(팝업전시 필요)
6. Pagination 적용

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 400  | VALIDATION_FAILED |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 3. Recruit

공고 작성, 조회, 댓글, 지원 기능을 제공한다.

---

# 3-1 공고 상세 조회

## Endpoint

```http
GET /recruits/{recruitId}
```

---

## Authentication

인증 불필요

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| recruitId | 공고 ID       |

---

## Response

### 200 OK

```json
{
    "data":{
        "recruitId":1,
        "title":"공모전 팀원 구합니다.",
        "authorId":10,
        "authorNickname":"작성자",
        "authorEXP":100,
        "createdAt":"2026-07-01T10:00:00+09:00",
        "startDate":"2026-07-05",
        "endDate":"2026-07-10",
        "category":[1],
        "topics":[1,4],
        "onCampus":true,
        "simpleDesc":"간단 소개",
        "content":"상세 내용",
        "status":"OPEN",
        "maxMembers":4,
        "availableSlots":2,
        "isScrapped":false
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Recruit 조회
2. DeletedAt 여부 확인
3. availableSlots 계산
4. 로그인 사용자인 경우 스크랩 여부(isScrapped) 계산 (비로그인 시 false)
5. 응답 반환

> **스크랩 등록/해제는 공고 상세에서만 가능하다.** 목록 카드에는 스크랩 버튼을 노출하지 않는다.
> 스크랩한 공고는 **마이페이지 > 활동 내역 > 스크랩한 공고**(3-16)에서 조회한다.

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 404  | RECRUIT_NOT_FOUND |

---

# 3-2 공고 작성

## Endpoint

```http
POST /recruits
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
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

---

## Response

### 201 Created

```json
{
    "data":{
        "recruitId":1
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 입력값 검증
3. Recruit 생성
4. Category 매핑
5. Topic 매핑

---

## Validation

| Field      | Rule           |
|:---------- |:-------------- |
| 제목         | 최소 2자 ~ 최대 20자 |
| 모집인원       | 최소 1명 ~ 최대 7명  |
| 간단소개       | 최대 50자         |
| 상세내용       | 필수, 최대 1000자    |
| onCampus   | 필수(Boolean)    |
| categoryIds | 최소 1개 이상       |

> 교내/교외 구분은 Category가 아니라 `onCampus`(Boolean)로 관리한다.
> Category 마스터에서 '교내' 항목은 제거되었다.

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 400  | VALIDATION_FAILED |

---

# 3-3 AI 공고 초안 생성

## Endpoint

```http
POST /recruits/ai-draft
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "simpleDesc":"광고 공모전",
    "content":"기본 내용"
}
```

| 필드         | 필수 | 설명                                    |
|:------------ |:---- |:--------------------------------------- |
| simpleDesc   | 선택 | 간단 소개 초안 (최대 50자)               |
| content      | 필수 | 상세 내용 초안 (최대 1000자, 공란 불가)   |

---

## Response

### 200 OK

```json
{
    "data":{
        "simpleDesc":"AI가 작성한 간단소개",
        "content":"AI가 작성한 상세내용"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 입력 데이터 검증 (content는 필수 — 미입력 시 AI 호출 없이 400 VALIDATION_FAILED)
2. AI 서버 호출 — 사용자가 입력한 simpleDesc/content 초안을 기반으로 문장을 다듬어 확장 (없는 사실을 임의로 지어내지 않음)
3. 초안 생성
4. 결과 반환

---

## Error

| HTTP | Error Code           |
|:---- |:-------------------- |
| 400  | VALIDATION_FAILED    |
| 500  | AI_GENERATION_FAILED |

---

# 3-4 공고 마감

## Endpoint

```http
PATCH /recruits/{recruitId}/close
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 작성자 확인
2. 모집 상태 변경 (`OPEN` 또는 `ADDITIONAL` → `CLOSED`)

CLOSED 상태에서는 신규 지원을 받지 않는다.

---

## Error

| HTTP | Error Code        | 설명       |
|:---- |:----------------- |:-------- |
| 403  | FORBIDDEN         | 작성자가 아님  |
| 404  | RECRUIT_NOT_FOUND | 공고 없음    |
| 409  | ALREADY_CLOSED    | 이미 마감된 공고 |

---

# 3-5 공고 추가 모집

## Endpoint

```http
PATCH /recruits/{recruitId}/additional
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 작성자 확인
2. 마감(`CLOSED`) 상태인지 확인
3. 연결된 프로젝트가 이미 종료(`Project.Status = END`)되었는지 확인
4. 상태를 `ADDITIONAL`(추가 모집)로 변경

팀 구성이 끝나 마감(`CLOSED`)했으나, 팀원 이탈 등으로 인원이 비었을 때 사용한다.

팀원이 나가더라도 상태는 자동으로 바뀌지 않는다. **공고 작성자(프로젝트장)가 직접 이 API를 호출**해야 추가 모집이 시작된다.

```
CLOSED ──(팀원 이탈로 인원 부족)──> ADDITIONAL
```

`ADDITIONAL` 상태에서는 다시 지원을 받을 수 있으며, 프론트에서는 '추가 모집' 배지로 구분 표시한다.

이미 프로젝트가 생성된 공고인 경우, 추가 모집으로 수락된 지원자는 기존 프로젝트와 그룹 채팅방에 합류한다.

단, 연결된 프로젝트가 이미 종료(6-4 프로젝트 종료)된 경우에는 더 이상 새 팀원을 받을 대상이 없으므로 추가 모집으로 전환할 수 없다.

---

## Error

| HTTP | Error Code                  | 설명                    |
|:---- |:---------------------------- |:---------------------- |
| 403  | FORBIDDEN                    | 작성자가 아님             |
| 404  | RECRUIT_NOT_FOUND             | 공고 없음                |
| 409  | ALREADY_ADDITIONAL            | 이미 추가 모집 중인 공고   |
| 409  | RECRUIT_NOT_CLOSED             | 마감(CLOSED) 상태가 아닌 공고 |
| 409  | PROJECT_ENDED_CANNOT_RECRUIT  | 연결된 프로젝트가 이미 종료됨 |

---

# 3-6 공고 삭제

## Endpoint

```http
DELETE /recruits/{recruitId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 작성자 확인
2. DeletedAt 기록(Soft Delete)

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | RECRUIT_NOT_FOUND |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 3-7 댓글 및 답글 목록 조회

## Endpoint

```http
GET /recruits/{recruitId}/comments
```

---

## Authentication

인증 불필요

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| recruitId | 공고 ID       |

---

## Response

### 200 OK

```json
{
    "data":{
        "comments":[
            {
                "commentId":1,
                "authorId":15,
                "authorNickname":"지원희망자",
                "content":"비전공자도 가능한가요?",
                "createdAt":"2026-07-02T14:30:00+09:00",
                "isAuthor":false,
                "replies":[
                    {
                        "commentId":2,
                        "authorId":10,
                        "authorNickname":"공고작성자",
                        "content":"네 가능합니다.",
                        "createdAt":"2026-07-02T15:00:00+09:00",
                        "isAuthor":true
                    }
                ]
            }
        ]
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 공고 존재 여부 확인
2. 부모 댓글 조회
3. 답글 조회
4. 계층 구조(Tree) 생성
5. Soft Delete 댓글은 내용만 "삭제된 댓글입니다."로 변경
6. 삭제 댓글의 작성자 정보는 제거
7. 답글 구조는 유지

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 404  | RECRUIT_NOT_FOUND |

---

## Related Documents

- ERROR_CODE.md

---

# 3-8 댓글 및 답글 작성

## Endpoint

```http
POST /recruits/{recruitId}/comments
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "content":"댓글 내용",
    "parentCommentId":null
}
```

parentCommentId

- null → 일반 댓글
- 숫자 → 답글

> 대댓글의 깊이 제한은 없다. 답글에 다시 답글을 다는 것도 허용한다.

---

## Response

### 201 Created

```json
{
    "data":{
        "commentId":3
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. Recruit 존재 여부 확인
3. 부모 댓글 존재 여부 확인(답글인 경우)
4. 댓글 생성
5. 댓글 작성자가 공고 작성자 본인이 아니면, 공고 작성자에게 알림(`type=COMMENT`, `referenceType=RECRUIT`, `referenceId=recruitId`) 발송 (`NotificationSetting.CommentNoti` OFF면 미발송)

---

## Validation

| Field   | Rule   |
|:------- |:------ |
| content | 2~100자 |

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 400  | VALIDATION_FAILED |
| 404  | RECRUIT_NOT_FOUND |
| 404  | COMMENT_NOT_FOUND |

---

# 3-9 댓글 삭제

## Endpoint

```http
DELETE /comments/{commentId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 작성자 확인
3. DeletedAt 기록
4. 실제 삭제는 하지 않음
5. 하위 답글은 유지

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | COMMENT_NOT_FOUND |

---

# 3-10 AI 지원서 초안 생성

## Endpoint

```http
POST /recruits/{recruitId}/applies/ai-draft
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "message":"원본 자기소개"
}
```

---

## Response

### 200 OK

```json
{
    "data":{
        "convertedText":"AI가 다듬은 지원 메시지"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 공고 조회
2. 사용자가 입력한 원본 메시지(`message`)를 AI가 공고 맥락에 맞게 **리터칭(다듬기)**
3. 결과 반환

> 이 기능은 **프로필(이력서)이 없어도 사용할 수 있다.** 사용자가 직접 쓴 메시지를 다듬는 것이므로 이력서를 필요로 하지 않는다.

---

## Error

| HTTP | Error Code           | 설명       |
|:---- |:-------------------- |:-------- |
| 400  | VALIDATION_FAILED    | 원본 메시지 없음 |
| 404  | RECRUIT_NOT_FOUND    | 존재하지 않는 공고 |
| 500  | AI_GENERATION_FAILED | AI 생성 실패 |

---

# 3-11 공고 지원하기

## Endpoint

```http
POST /recruits/{recruitId}/applies
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "message":"자기소개 및 지원 동기"
}
```

---

## Response

204 No Content

---

### Field

| Name       | Type       | Required | Description                          |
|:---------- |:---------- |:-------- |:------------------------------------ |
| message    | String     | O        | 전송 메시지 (300자 이하)                     |
| keywordIds | List<Long> | X        | 지원 키워드 ID 목록 (**전체 통틀어 최대 5개**)       |

> 키워드는 `GET /apply-keywords`로 받아 Nested Dropdown으로 선택한다.

## Business Logic

1. Access Token 검증
2. Recruit 조회
3. 모집 상태 확인 (`OPEN` 또는 `ADDITIONAL`만 지원 가능)
4. 중복 지원 여부 확인
5. 지원 메시지 검증
6. Apply 생성
7. 작성자에게 알림 발송

---

## Validation

| Field   | Rule    |
|:------- |:------- |
| message | 최대 300자 |

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 400  | VALIDATION_FAILED |
| 400  | RECRUIT_CLOSED    |
| 404  | RECRUIT_NOT_FOUND |
| 409  | ALREADY_APPLIED   |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 3-12 공고 수정

## Endpoint

```http
PATCH /recruits/{recruitId}
```

---

## Authentication

Bearer Access Token

---

## Request Body

공고 작성과 동일

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 작성자 확인
3. 입력값 검증
4. Recruit 수정
5. Category 매핑 갱신
6. Topic 매핑 갱신

## 상태 변경

공고 수정 화면에서 내용 수정과 함께 **모집 상태를 변경**할 수 있다.
상태 변경은 상태에 따라 아래 API로 이어진다.

| 현재 상태      | 가능한 변경        | API                                 |
|:---------- |:------------- |:----------------------------------- |
| OPEN(모집중)  | 마감하기          | `PATCH /recruits/{recruitId}/close` |
| CLOSED(마감) | 추가모집하기        | `PATCH /recruits/{recruitId}/additional` |
| ADDITIONAL(추가모집) | 마감하기      | `PATCH /recruits/{recruitId}/close` |

```
모집중 ──마감하기──> 마감 ──추가모집하기──> 추가모집 ──마감하기──> 마감
```

---

## Validation

공고 작성과 동일

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 400  | VALIDATION_FAILED |
| 403  | FORBIDDEN         |
| 404  | RECRUIT_NOT_FOUND |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 3-13 지원 취소

## Endpoint

```http
DELETE /applies/{applyId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 본인이 지원한 지원서인지 확인
3. WAITING 상태인 경우만 취소 가능
4. Apply 삭제

---

## Error

| HTTP | Error Code              |
|:---- |:----------------------- |
| 403  | FORBIDDEN               |
| 404  | APPLY_NOT_FOUND   |
| 409  | APPLY_NOT_WAITING |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 3-14 공고 스크랩

## Endpoint

```http
POST /recruits/{recruitId}/scrap
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| recruitId | 공고 ID       |

---

## Response

### 201 Created

```json
{
    "data":{
        "recruitId":1,
        "isScrapped":true
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. Recruit 존재 여부 확인
3. 이미 스크랩했는지 확인
4. RecruitScrap 생성

---

## Error

| HTTP | Error Code        | 설명         |
|:---- |:----------------- |:---------- |
| 404  | RECRUIT_NOT_FOUND | 공고 없음      |
| 409  | ALREADY_SCRAPPED  | 이미 스크랩한 공고 |

---

# 3-15 공고 스크랩 취소

## Endpoint

```http
DELETE /recruits/{recruitId}/scrap
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. RecruitScrap 존재 여부 확인
3. RecruitScrap 삭제 (Hard Delete)

---

## Error

| HTTP | Error Code        | 설명          |
|:---- |:----------------- |:----------- |
| 404  | RECRUIT_NOT_FOUND | 공고 없음       |
| 404  | SCRAP_NOT_FOUND   | 스크랩하지 않은 공고 |

---

# 3-16 스크랩한 공고 목록 조회

## Endpoint

```http
GET /users/me/scraps
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name | Type    | Required | Description    |
|:---- |:------- |:-------- |:-------------- |
| page | Integer | X        | 기본값 0          |
| size | Integer | X        | 기본값 10         |
| sort | String  | X        | createdAt,desc |

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "recruitId":1,
                "title":"제일기획 공모전 팀원 모집",
                "authorId":10,
                "authorNickname":"픽키",
                "onCampus":true,
                "status":"OPEN",
                "maxMembers":4,
                "availableSlots":2,
                "scrappedAt":"2026-07-05T10:00:00+09:00"
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":3,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자의 RecruitScrap 조회
3. 삭제된 공고(DeletedAt)는 제외
4. Pagination 적용

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 4. User & Feedback

회원 프로필, 지원 현황, 공고 관리 및 상호평가 기능을 제공한다.

---

# 4-1 내 프로필 조회

## Endpoint

```http
GET /users/me
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "nickname":"픽키",
        "univId":1,
        "univ":"명지대학교",
        "major":"융합소프트웨어학부 데이터사이언스전공",
        "academicStatus":"ENROLLED",
        "hope":"백엔드 개발자",
        "strength":"책임감, 꼼꼼한 일정 관리",
        "aboutMe":"AI가 생성한 자기소개",
        "exp":500,
        "topic":[1,2,3],
        "skillTool":[
            {
                "techStackName":"Java",
                "level":3
            }
        ],
        "license":[
            {
                "licenseName":"정보처리기사",
                "date":"2026-05"
            }
        ],
        "experience":[
            {
                "startDate":"2026-01",
                "endDate":"2026-06",
                "title":"인턴",
                "organization":"회사",
                "description":"업무내용"
            }
        ],
        "additionalLink":[
            {
                "linkName":"Github",
                "url":"https://github.com/..."
            }
        ]
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. Resume 조회
3. 기간 포맷 변환
4. 응답 반환

---

## Error

| HTTP | Error Code       |
|:---- |:---------------- |
| 404  | RESUME_NOT_FOUND |

---

# 4-2 프로필 생성

## Endpoint

```http
POST /users/create-resume
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "univId":1,
    "major":"데이터사이언스",
    "academicStatus":"ENROLLED",
    "hope":"백엔드 개발자",
    "strength":"책임감",
    "topic":[1,2],
    "skillTool":[
        {
            "techStackName":"Java",
            "level":3
        }
    ],
    "license":[
        {
            "licenseName":"정보처리기사",
            "date":"2026-05"
        }
    ],
    "experience":[
        {
            "startDate":"2026-01",
            "endDate":"2026-06",
            "title":"인턴",
            "organization":"회사",
            "description":"업무"
        }
    ],
    "additionalLink":[
        {
            "linkName":"Github",
            "url":"https://github.com/..."
        }
    ]
}
```

### Field

| Name           | Type   | Required | Description                                                   | 저장 위치                |
|:-------------- |:------ |:-------- |:------------------------------------------------------------- |:-------------------- |
| univId         | Long   | O        | 대학교 ID (마스터에서 **선택**, `GET /universities`)                    | `MemberUniv.UnivId`  |
| major          | String | O        | 전공 (2~50자, 사용자 **직접 입력**)                                     | `MemberUniv.Major`   |
| academicStatus | Enum   | O        | ENROLLED / LEAVE_OF_ABSENCE / GRADUATION_DEFERRED / GRADUATED | `MemberUniv.Status`  |
| hope           | String | X        | 희망 진로 (100자 이하)                                              | `MemberResume.Hope`  |
| strength       | String | X        | 장점 (300자 이하)                                                  | `MemberResume.Strength` |
| topic          | Array  | X        | 관심 주제 ID 목록 (**최대 3개**)                                        | `DetailTopic`        |
| skillTool      | Array  | X        | 기술 스택 + 숙련도(1~3)                                              | `MemberTechStack`    |
| license        | Array  | X        | 자격증 + 취득일자                                                    | `MemberLicense`      |
| experience     | Array  | X        | 수상 및 경험                                                       | `DetailExperience`   |
| additionalLink | Array  | X        | 외부 링크 (Github, Notion 등)                                      | `AdditionalLink`     |

### 프로필 = 이력서 (Resume)

본 서비스에서 **프로필과 이력서는 같은 것**이다. 마이페이지의 프로필 탭에서 이력서 형태로 조회된다.

### AboutMe 생성 정책

| 시점       | 동작                                                                     |
|:-------- |:---------------------------------------------------------------------- |
| 최초 생성 시  | 입력한 항목들을 기반으로 **AI가 `aboutMe`만 자동 생성**한다. 사용자는 직접 입력하지 않는다.             |
| 생성 이후    | 사용자가 **이력서를 직접 수정**한다. `aboutMe`도 직접 수정하며 **AI 재생성은 하지 않는다.**            |

즉 AI는 최초 1회 자기소개 초안을 만들어주는 역할만 하며, 이후 이력서 관리 주체는 사용자다.

> 프로필 이미지와 학년(Grade)은 사용하지 않는다. 학적 상태(academicStatus)만 사용한다.

---

## Response

### 201 Created

```json
{
    "data":{
        "message":"Resume Created"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 입력값 검증
3. MemberUniv / MemberResume / DetailTopic / MemberTechStack / MemberLicense / DetailExperience / AdditionalLink 저장
4. 저장된 입력 정보를 기반으로 **AI가 AboutMe 자동 생성**
5. `MemberResume.AboutMe` 저장

---

## Error

| HTTP | Error Code            |
|:---- |:--------------------- |
| 409  | RESUME_ALREADY_EXISTS |
| 500  | AI_GENERATION_FAILED  |

---

# 4-3 프로필(이력서) 수정

## Endpoint

```http
PATCH /users/me
```

---

## Authentication

Bearer Access Token

---

## Request Body

프로필 생성과 동일

---

### Field

프로필 생성(4-2)과 동일한 항목에 더해, **`aboutMe`를 직접 수정**할 수 있다.

| Name    | Type   | Required | Description                    |
|:------- |:------ |:-------- |:------------------------------- |
| aboutMe | String | X        | 자기소개 (최초 생성 시 AI가 만든 초안을 직접 수정) |

> 수정 시 **AI 재생성은 수행하지 않는다.** 최초 생성 이후 이력서는 사용자가 직접 관리한다.

---

## Response

204 No Content

---

## Business Logic

1. Resume 조회
2. 수정
3. 저장

---

## Error

| HTTP | Error Code       |
|:---- |:---------------- |
| 404  | RESUME_NOT_FOUND |

---

# 4-4 지원 현황 조회

## Endpoint

```http
GET /users/me/applies
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "applyId":7,
                "recruitId":1,
                "recruitTitle":"제일기획 공모전 팀원 모집",
                "recruitStatus":"OPEN",
                "status":"WAITING",
                "keywords":[
                    { "keywordId":5, "content":"마감기한 잘 지켜요" },
                    { "keywordId":9, "content":"꼼꼼하게 마무리해요" }
                ],
                "createdAt":"2026-07-02T14:30:00+09:00"
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":3,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### Field

| Name         | Type              | Description                          |
|:------------ |:----------------- |:------------------------------------ |
| applyId      | Long               | 지원 ID                                |
| recruitId    | Long               | 공고 이동용 ID ('공고 글 바로가기')              |
| recruitTitle | String             | 지원한 공고의 제목                          |
| recruitStatus| Enum               | 공고 상태 (OPEN / CLOSED / ADDITIONAL)   |
| status       | Enum               | 지원 상태 (WAITING / ACCEPTED / REJECTED) |
| keywords     | List<KeywordItem>  | 지원 시 선택한 지원 키워드 (텍스트 포함, 5-7과 동일 형태) |
| createdAt    | String             | 지원 일시                              |

`KeywordItem`: `{ "keywordId": Long, "content": String }`

---

## Business Logic

1. 로그인 사용자 조회
2. Apply 목록 조회
3. 각 Apply에 매핑된 지원 키워드를 함께 조회 (ApplyKeywordMap → ApplyKeyword)
4. Pagination 적용

---

# 4-5 작성한 공고 조회

## Endpoint

```http
GET /users/me/recruits
```

---

## Authentication

Bearer Access Token

---

## Response

Pagination Response

Recruit 목록 반환

---

## Business Logic

1. 작성한 Recruit 조회
2. Pagination 적용

---

# 4-6 작성한 댓글 조회

## Endpoint

```http
GET /users/me/comments
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name | Type    | Required | Description    |
|:---- |:------- |:-------- |:-------------- |
| page | Integer | X        | 기본값 0          |
| size | Integer | X        | 기본값 10         |
| sort | String  | X        | createdAt,desc |

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "commentId":5,
                "recruitId":1,
                "recruitTitle":"제일기획 공모전 팀원 모집",
                "recruitStatus":"OPEN",
                "content":"비전공자도 가능한가요?",
                "createdAt":"2026-07-02T14:30:00+09:00"
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":3,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### Field

| Name          | Type   | Description                        |
|:------------- |:------ |:---------------------------------- |
| recruitId     | Long   | 공고 이동용 ID ('공고 글 바로가기')            |
| recruitTitle  | String | 댓글을 단 공고의 제목                       |
| recruitStatus | Enum   | 공고 상태 (OPEN / CLOSED / ADDITIONAL)  |
| content       | String | 내가 작성한 댓글 내용                       |
| createdAt     | String | 댓글 작성 일시                           |

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자가 작성한 댓글/답글 조회
3. 삭제된 댓글(`DeletedAt`)은 제외
4. 삭제된 공고의 댓글은 제외
5. Pagination 적용

---

# 4-7 지원자 목록 조회

## Endpoint

```http
GET /recruits/{recruitId}/applicants
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "applyId":7,
                "memberId":12,
                "nickname":"pickii",
                "message":"열심히 하겠습니다!",
                "keywords":[
                    { "keywordId":5, "content":"마감기한 잘 지켜요" },
                    { "keywordId":9, "content":"꼼꼼하게 마무리해요" }
                ],
                "status":"WAITING",
                "createdAt":"2026-07-02T14:30:00+09:00"
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":3,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### Field

| Name      | Type              | Description                          |
|:--------- |:----------------- |:------------------------------------ |
| applyId   | Long               | 지원 ID                                |
| memberId  | Long               | 지원자 회원 ID                            |
| nickname  | String             | 지원자 닉네임                              |
| message   | String             | 지원 메시지                              |
| keywords  | List<KeywordItem>  | 지원자가 선택한 지원 키워드 (텍스트 포함, 5-7과 동일 형태) |
| status    | Enum               | 지원 상태 (WAITING / ACCEPTED / REJECTED) |
| createdAt | String             | 지원 일시                              |

`KeywordItem`: `{ "keywordId": Long, "content": String }`

---

## Business Logic

1. 작성자 여부 확인
2. 지원자 조회
3. 각 지원자에게 매핑된 지원 키워드를 함께 조회 (ApplyKeywordMap → ApplyKeyword)
4. Pagination 적용

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | RECRUIT_NOT_FOUND |

---

# 4-8 지원자 수락 / 거절

## Endpoint

```http
PATCH /applies/{applyId}/status
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "status":"ACCEPTED"
}
```

---

## Response

204 No Content

---

## Business Logic

1. 작성자 확인
2. 최대 인원 확인
3. 상태 변경 (`WAITING` → `ACCEPTED` / `REJECTED`)
4. `ACCEPTED`인 경우 `Recruit.CurrentCount` 증가
5. **이미 Project가 생성된 공고라면**, 수락 즉시 ProjectMember로 등록하고 그룹 채팅방에 자동 초대한다 (공고 상태와 무관)
6. Notification 생성

---

## Error

| HTTP | Error Code        | 설명               |
|:---- |:------------------ |:----------------- |
| 400  | VALIDATION_FAILED  | status가 ACCEPTED/REJECTED가 아님 |
| 403  | FORBIDDEN          | 공고 작성자가 아님        |
| 409  | RECRUIT_FULL       | 모집 정원 마감          |
| 409  | APPLY_NOT_WAITING  | 이미 처리된 지원건 (재처리 불가) |

---

# 4-9 평가 대상 팀원 조회

## Endpoint

```http
GET /feedbacks/projects/{projectId}/members
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| projectId | 프로젝트 ID     |

---

## Response

### 200 OK

```json
{
    "data":{
        "projectId":1,
        "evaluationDeadline":"2026-08-13T23:59:59+09:00",
        "members":[
            {
                "memberId":2,
                "nickname":"팀원A",
                "isEvaluated":false
            }
        ]
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 프로젝트가 `END` 상태인지 확인
3. 평가 기간(종료 후 3일) 내인지 확인
4. 종료 시점 기준 참여 팀원(`LeftAt IS NULL`) 중 본인을 제외한 목록 조회
5. 평가 여부(isEvaluated) 계산

---

## Error

| HTTP | Error Code                 | 설명              |
|:---- |:-------------------------- |:--------------- |
| 403  | FORBIDDEN                  | 프로젝트 참여자가 아님    |
| 404  | PROJECT_NOT_FOUND          | 프로젝트 없음         |
| 409  | PROJECT_NOT_ENDED          | 아직 종료되지 않은 프로젝트 |
| 409  | EVALUATION_PERIOD_EXPIRED  | 평가 기간(3일) 종료    |

---

# 4-10 상호평가 작성

## Endpoint

```http
POST /feedbacks
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "projectId":1,
    "revieweeId":2,
    "scores":{
        "responsibility":5,
        "communication":4,
        "deadline":5,
        "cooperation":4,
        "contribution":5
    },
    "strength":"장점",
    "weakness":"개선점"
}
```

### Field

| Name       | Type   | Required | Description                  |
|:---------- |:------ |:-------- |:---------------------------- |
| projectId  | Long   | O        | 평가 대상 프로젝트 ID                |
| revieweeId | Long   | O        | 평가 대상 팀원 ID                  |
| scores     | Object | O        | 5개 항목 각 1~5점                 |
| strength   | String | O        | 주관식 협업 후기 - 장점 (30~500자)      |
| weakness   | String | O        | 주관식 협업 후기 - 개선점 (30~500자)     |

> 협업 후기는 **장점 / 개선점 두 개로 나누어** 입력받는다. (DB `FeedBack.SText` / `WText`)

---

## Response

201 Created

---

## Business Logic

1. 프로젝트 참여 확인
2. 프로젝트가 `END` 상태인지 확인
3. 평가 기간(종료 후 3일) 내인지 확인
4. 평가 중복 확인 (Unique: ProjectId + ReviewerId + RevieweeId)
5. Feedback 저장

---

## Error

| HTTP | Error Code                | 설명              |
|:---- |:------------------------- |:--------------- |
| 400  | VALIDATION_FAILED         | 입력값 검증 실패       |
| 400  | ALREADY_EVALUATED         | 이미 평가 완료        |
| 403  | FORBIDDEN                 | 평가 권한 없음        |
| 404  | PROJECT_NOT_FOUND         | 프로젝트 없음         |
| 404  | PROJECT_MEMBER_NOT_FOUND  | 평가 대상이 팀원이 아님   |
| 409  | PROJECT_NOT_ENDED         | 아직 종료되지 않은 프로젝트 |
| 409  | EVALUATION_PERIOD_EXPIRED | 평가 기간(3일) 종료    |

---

# 4-11 AI 피드백 목록 조회

## Endpoint

```http
GET /feedbacks
```

---

## Authentication

Bearer Access Token

---

## Response

Pagination Response

내가 참여했던 종료 프로젝트 목록과 AI 피드백 생성 여부를 반환

```json
{
    "data":{
        "content":[
            {
                "projectId":1,
                "name":"제일기획 공모전 팀",
                "evaluationPeriod":{
                    "start":"2026-08-10",
                    "end":"2026-08-13"
                },
                "remainingDays":2,
                "memberCount":4,
                "evaluatedCount":2,
                "requiredCount":2,
                "isAiFeedbackAvailable":true
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":1,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

### Field

| Name                  | Type    | Description                            |
|:--------------------- |:------- |:-------------------------------------- |
| name                  | String  | 프로젝트명 (= 그룹 채팅방 이름)                    |
| evaluationPeriod      | Object  | 평가 기간 (프로젝트 종료일 ~ +3일)                 |
| remainingDays         | Integer | 평가 마감까지 남은 일수                           |
| memberCount           | Integer | 종료 시점 팀원 수 (N)                          |
| evaluatedCount        | Integer | **나를 평가한 팀원 수**                         |
| requiredCount         | Integer | AI 피드백 생성에 필요한 최소 평가 인원 (`ceil(N/2)`)   |
| isAiFeedbackAvailable | Boolean | 피드백 확인 가능 여부                            |

프론트에서는 `evaluatedCount / memberCount` 형식(예: 2/4)으로 표시한다.

---

## Business Logic

1. 종료(`END`)된 프로젝트 중 참여했던 프로젝트 조회
2. 프로젝트별 나를 평가한 팀원 수(evaluatedCount) 집계
3. 최소 평가 인원(requiredCount) 계산
4. AI 피드백 존재 및 확인 가능 여부 판단
5. Pagination 적용

---

# 4-12 AI 피드백 상세 조회

## Endpoint

```http
GET /feedbacks/ai/{projectId}
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| projectId | 프로젝트 ID     |

---

## Response

### 200 OK

```json
{
    "data":{
        "projectId":1,
        "keywords":[
            "#꼼꼼한",
            "#책임감"
        ],
        "strengthSummary":"장점 요약",
        "weaknessSummary":"개선점 요약"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## AI 피드백 생성 정책

상호평가는 **프로젝트 종료(`Project.Status = END`) 시점부터 3일간** 가능하다.

### 생성 트리거

AI 피드백은 다음 **두 트리거 중 먼저 도달하는 시점**에 생성된다.

| 트리거   | 조건                | 처리 방식      |
|:----- |:----------------- |:---------- |
| 조기 완료 | 팀원 전원이 평가를 완료한 경우 | 즉시 생성 (이벤트) |
| 기간 만료 | 종료 후 3일이 경과한 경우   | 배치로 생성     |

```
프로젝트 종료 (END)

↓

평가 기간 3일 시작

├── 전원 평가 완료 → 즉시 AI 피드백 생성
│
└── 3일 경과      → 평가 마감 후 배치로 AI 피드백 생성
```

전원이 일찍 평가를 마치면 3일을 기다리지 않는다.

AI 피드백이 생성되면(두 트리거 중 어느 쪽이든), 평가 대상 본인에게 알림(`type=FEEDBACK`, `referenceType=FEEDBACK`, `referenceId=projectId`)을 발송한다. `NotificationSetting.ProjectNoti`가 OFF면 미발송.

### 평가 대상 인원 (기준 N)

평가 대상과 팀 인원(N)은 **프로젝트 종료(END) 시점에 참여 중이던 팀원**으로 확정한다.

- 중도 이탈/퇴출된 팀원(`ProjectMember.LeftAt IS NOT NULL`)은 평가자·평가 대상 모두에서 제외한다.
- '전원 평가 완료' 트리거도 이 인원 기준으로 판단하므로, 이탈자가 있어도 남은 팀원이 모두 평가하면 즉시 생성된다.

```
5인 팀에서 1명 퇴출 → 종료 시점 팀원 4명

→ N = 4, 최소 평가 인원 = 2명
→ 남은 4명이 모두 평가 완료 시 즉시 생성
```

두 경우 모두 아래 **최소 평가 인원 조건**을 만족해야 생성된다.

### 최소 평가 인원

| 팀 인원(N) | AI 피드백 생성에 필요한 최소 평가 인원 |
|:------- |:---------------------- |
| 2       | 생성 대상 아님 (익명성 확보 불가)    |
| 3       | 2명 이상                   |
| 4       | 2명 이상                   |
| 5       | 3명 이상                   |
| N (≥3)  | `ceil(N / 2)` 명 이상       |

- 2인 팀은 평가자가 1명뿐이라 익명성이 보장되지 않으므로 AI 피드백 대상에서 제외한다.
- 3일 경과 시점에 조건을 만족한 팀원만 생성하며, 미달한 팀원은 생성하지 않고 `INSUFFICIENT_EVALUATION`을 반환한다.

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 프로젝트 종료(`END`) 확인
3. 평가 기간(3일) 종료 확인
4. 최소 평가 인원 조건 충족 확인
5. AI 요약 및 키워드 조회
6. 결과 반환

---

## Error

| HTTP | Error Code               | 설명                          |
|:---- |:------------------------ |:--------------------------- |
| 403  | FORBIDDEN                | 프로젝트 참여자가 아님                |
| 404  | PROJECT_NOT_FOUND        | 프로젝트 없음                     |
| 404  | EVALUATION_NOT_FOUND     | AI 피드백 데이터 없음               |
| 409  | EVALUATION_NOT_COMPLETE  | 평가 기간이 아직 끝나지 않음            |
| 409  | INSUFFICIENT_EVALUATION  | 최소 평가 인원 미달 (2인 팀 포함)        |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 5. Master Data

프론트엔드 드롭다운 등에서 사용할 기준 데이터를 조회하는 API이다.

DB의 Master Table과 1:1로 대응한다.

| API                  | 대응 테이블         |
|:-------------------- |:-------------- |
| GET /categories      | `Category`     |
| GET /topics          | `Topic`        |
| GET /tech-stacks     | `TechStack`    |
| GET /licenses        | `License`      |
| GET /link-categories | `LinkCategory` |
| GET /keywords        | `Keyword`      |
| GET /universities    | `Univ`         |
| GET /apply-keywords  | `ApplyKeyword` (Nested) |

모두 인증이 불필요하며, 데이터 변경이 거의 없으므로 캐싱을 적용한다.

---

# 5-1 카테고리 조회

## Endpoint

```http
GET /categories
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "categoryId":1,
            "name":"공모전"
        },
        {
            "categoryId":2,
            "name":"프로젝트"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

> '교내' 항목은 Category에서 제거되었다. 교내/교외 구분은 공고의 `onCampus`(Boolean)를 사용한다.

---

## Business Logic

1. Category 전체 목록 조회
2. 결과 반환

---

# 5-2 주제 조회

## Endpoint

```http
GET /topics
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "topicId":1,
            "name":"백엔드"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Topic 전체 목록 조회
2. 결과 반환

---

# 5-3 기술 스택 조회

## Endpoint

```http
GET /tech-stacks
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "techStackId":1,
            "name":"Java",
            "type":"SKILL"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. TechStack 전체 목록 조회
2. 결과 반환

---

# 5-4 자격증 조회

## Endpoint

```http
GET /licenses
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "licenseId":1,
            "name":"정보처리기사"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. License 전체 목록 조회
2. 결과 반환

---

# 5-5 링크 카테고리 조회

## Endpoint

```http
GET /link-categories
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "linkCategoryId":1,
            "name":"Github",
            "picUrl":"https://cdn.pickii.com/link/github.png"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. LinkCategory 전체 목록 조회
2. 결과 반환

프로필의 `additionalLink` 입력 시 플랫폼 선택 드롭다운에 사용한다.

---

# 5-6 피드백 키워드 조회

## Endpoint

```http
GET /keywords
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "keywordId":1,
            "name":"원활한 소통"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Keyword 전체 목록 조회
2. 결과 반환

AI 상호평가 결과(`AIFeedBackKeyword`)에서 사용하는 키워드 풀이다.

---

## Related Documents

- DB_SCHEMA.md
- ENUM.md

---

# 5-7 지원 키워드 조회

## Endpoint

```http
GET /apply-keywords
```

---

## Authentication

인증 불필요

---

## Response

### 200 OK

카테고리 안에 키워드가 중첩된 **Nested** 구조로 반환한다.

```json
{
    "data":[
        {
            "categoryId":1,
            "category":"실행력 / 책임감",
            "keywords":[
                { "keywordId":1, "content":"마감기한 잘 지켜요" },
                { "keywordId":2, "content":"책임감 강해요" },
                { "keywordId":3, "content":"끝까지 완주해요" }
            ]
        },
        {
            "categoryId":2,
            "category":"기획 / 아이디어",
            "keywords":[
                { "keywordId":10, "content":"아이디어 뱅크예요" },
                { "keywordId":11, "content":"기획력 좋아요" }
            ]
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. ApplyKeywordCategory 전체 조회
2. 각 카테고리에 속한 ApplyKeyword를 중첩하여 반환

## 정책

- 프론트는 **Nested Dropdown**(카테고리 열기 → 키워드 선택)으로 표시한다.
- 지원 시 **전체 카테고리를 통틀어 최대 5개**까지 선택할 수 있다.

---

## Related Documents

- DB_SCHEMA.md

---

# 5-8 대학교 목록 조회

## Endpoint

```http
GET /universities
```

---

## Authentication

인증 불필요

---

## Query Parameter

| Name    | Type   | Required | Description                    |
|:------- |:------ |:-------- |:------------------------------ |
| keyword | String | X        | 학교명 검색어 (부분 일치, 미지정 시 전체 목록 반환) |

학교 수가 많으므로 프론트에서는 **검색형 드롭다운(자동완성)** 으로 구현한다.

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "univId":1,
            "name":"명지대학교"
        },
        {
            "univId":2,
            "name":"연세대학교"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. keyword가 있으면 학교명 부분 일치 검색, 없으면 전체 목록 조회
2. 결과 반환

## 정책

- 사용자는 목록에서 **선택만** 하며 학교명을 직접 입력하지 않는다.
- **캠퍼스는 구분하지 않는다.** 인문/자연, 서울/지역 캠퍼스는 모두 하나의 학교로 취급한다.
- 학교명이 통일되므로 `univId` 비교만으로 교내(onCampus) 여부를 정확히 판별할 수 있다.
- 전공(major)은 마스터로 관리하지 않고 **사용자가 직접 입력**한다.

---

## Related Documents

- DB_SCHEMA.md

---

# 6. Project

지원이 수락된 이후, 실제 팀 활동을 진행할 때 사용하는 API이다.

> **별도의 '내 프로젝트' 목록 화면은 제공하지 않는다.**
> 프로젝트 관리는 **그룹 채팅방을 통해 접근**한다. (채팅 목록의 그룹 탭 = 사실상 내 프로젝트 목록)
> 그룹 채팅방을 나가면 프로젝트에서도 탈퇴한 것으로 간주한다. (8-7 참고)

## 프로젝트 전환 시점

Project는 자동 생성되지 않는다.

**공고 작성자가 그룹 채팅을 생성하는 시점**에 Recruit가 Project로 전환된다.

```
공고(OPEN) → 지원자 수락(ACCEPTED) → 작성자가 그룹 채팅 생성

↓

Project 생성 (IN_PROGRESS) + ChatRoom(GROUP) + ProjectMember 등록
Recruit.Status 변경 없음 (공고는 계속 모집 가능)

↓

팀 활동 중 팀원 이탈 → Recruit.Status = ADDITIONAL (추가 모집)

↓

진행기간 종료 또는 프로젝트장이 종료 → Project.Status = END

↓

상호평가
```

---

# 6-1 프로젝트 생성 (그룹 채팅 생성)

## Endpoint

```http
POST /recruits/{recruitId}/project
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| recruitId | 공고 ID       |

---

## Request Body

```json
{
    "name":"제일기획 공모전 팀"
}
```

### Field

| Name | Type   | Required | Description             |
|:---- |:------ |:-------- |:----------------------- |
| name | String | O        | 팀명 / 프로젝트명 (그룹 채팅방 제목) |

---

## Response

### 201 Created

```json
{
    "data":{
        "projectId":1,
        "chatRoomId":10,
        "recruitStatus":"CLOSED",
        "memberCount":3
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 공고 작성자 여부 확인
3. 이미 Project가 생성되었는지 확인
4. `ACCEPTED` 상태 지원자가 1명 이상 존재하는지 확인
5. Project 생성 (`Status = IN_PROGRESS`)
6. ChatRoom 생성 (`Type = GROUP`, `ProjectId` 매핑)
7. 작성자를 ProjectMember로 등록 (`IsLeader = true`)
8. `ACCEPTED` 지원자를 ProjectMember로 등록
9. 모든 팀원을 ChatRoomMember로 등록
10. **`Recruit.Status`는 변경하지 않는다.** (공고는 그대로 모집을 이어갈 수 있다)
11. 팀원에게 알림 발송

프로젝트 생성 직후, 각 팀원은 이 프로젝트의 팀 일정을 자신의 캘린더에서 어떤 색상으로 볼지
개인 카테고리로 지정한다. (7-15 참고)

> **그룹 채팅방을 미리 개설해두고 계속 모집할 수 있다.** 개설이 공고를 마감시키지 않는다.
> 프로젝트 생성 이후 모집중(OPEN)/추가모집(ADDITIONAL) 상태에서 수락되는 지원자는
> **자동으로 기존 그룹 채팅방과 Project에 합류**한다.
> 공고 마감은 진행기간 만료 또는 작성자의 수동 마감(3-4)으로만 이루어진다.
> 프로젝트 생성에는 `ACCEPTED` 지원자가 최소 1명 필요하다. (혼자서는 생성 불가)

---

## Error

| HTTP | Error Code            | 설명                    |
|:---- |:--------------------- |:--------------------- |
| 403  | FORBIDDEN             | 공고 작성자가 아님            |
| 404  | RECRUIT_NOT_FOUND     | 공고 없음                 |
| 409  | PROJECT_ALREADY_EXISTS | 이미 프로젝트가 생성된 공고       |
| 409  | NO_ACCEPTED_APPLICANT | 수락된 지원자가 없음           |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 6-2 프로젝트 상세 조회

## Endpoint

```http
GET /projects/{projectId}
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description |
|:--------- |:----------- |
| projectId | 프로젝트 ID     |

---

## Response

### 200 OK

```json
{
    "data":{
        "projectId":1,
        "title":"제일기획 공모전",
        "status":"IN_PROGRESS",
        "startDate":"2026-07-10",
        "endDate":"2026-08-10",
        "leaderId":10
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 프로젝트 참여 여부 확인
3. Project 상세 조회

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | PROJECT_NOT_FOUND |

---

# 6-3 프로젝트 팀원 조회

## Endpoint

```http
GET /projects/{projectId}/members
```

---

## Authentication

Bearer Access Token

---

## Response

Pagination Response

Project Member 목록 반환

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 팀원 목록 조회
3. Pagination 적용

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | PROJECT_NOT_FOUND |

---

# 6-4 프로젝트 종료

## Endpoint

```http
PATCH /projects/{projectId}/close
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트장(Leader) 여부 확인
2. `Project.Status = END` 로 변경
3. 상호평가 가능 상태로 전환
4. 팀원에게 알림 발송

## 종료 정책

진행기간이 끝나도 즉시 종료되지 않는다. 프로젝트장에게 먼저 확인 알림을 보낸다.

```
진행기간(EndDate) 도달

↓

배치가 프로젝트장에게 종료 확인 알림 발송 (EndCheckedAt 기록)

↓

프로젝트장 선택

├── 연장  → PATCH /projects/{projectId}/extend  (EndDate 갱신, EndCheckedAt 초기화)
├── 종료  → PATCH /projects/{projectId}/close   (즉시 END)
└── 무응답 → 3일 경과 시 배치가 자동으로 END 전환
```

| 구분    | 조건                                             |
|:----- |:---------------------------------------------- |
| 수동 종료 | 프로젝트장이 본 API를 호출 (진행기간 전에도 언제든 가능)             |
| 자동 종료 | 종료 확인 알림 발송 후 3일(`EndCheckedAt + 3d`) 내 무응답 시 배치 |

Project 상태는 `IN_PROGRESS`, `END` 두 가지만 사용한다.

---

## Error

| HTTP | Error Code        | 설명         |
|:---- |:----------------- |:---------- |
| 403  | FORBIDDEN         | 프로젝트장이 아님  |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음    |
| 409  | ALREADY_ENDED     | 이미 종료된 프로젝트 |

---

# 6-5 프로젝트 진행기간 연장

## Endpoint

```http
PATCH /projects/{projectId}/extend
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "endDate":"2026-09-30"
}
```

### Field

| Name    | Type | Required | Description                    |
|:------- |:---- |:-------- |:------------------------------ |
| endDate | Date | O        | 새로운 종료일 (기존 EndDate 이후여야 함)    |

---

## Response

### 200 OK

```json
{
    "data":{
        "projectId":1,
        "endDate":"2026-09-30",
        "status":"IN_PROGRESS"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 프로젝트장(Leader) 여부 확인
3. 프로젝트가 `IN_PROGRESS` 상태인지 확인 (종료된 프로젝트는 연장 불가)
4. 새 `endDate`가 기존 `EndDate` 이후인지 검증
5. `Project.EndDate` 갱신
6. `Project.EndCheckedAt = NULL` 로 초기화 (3일 자동 종료 타이머 해제)
7. 팀원에게 연장 알림 발송

> 종료 확인 알림을 받은 뒤 3일이 지나면 자동으로 종료되므로, 연장은 그 안에 수행해야 한다.
> 새 `EndDate`에 도달하면 다시 종료 확인 알림이 발송된다.

---

## Error

| HTTP | Error Code        | 설명                      |
|:---- |:----------------- |:----------------------- |
| 400  | VALIDATION_FAILED | 기존 종료일보다 이전 날짜          |
| 403  | FORBIDDEN         | 프로젝트장이 아님               |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음                 |
| 409  | ALREADY_ENDED     | 이미 종료된 프로젝트는 연장할 수 없음   |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 6-6 프로젝트 나가기

> 그룹 채팅방 나가기(8-7)와 동일한 동작이다. 두 진입점 중 어느 쪽을 사용해도 결과는 같다.

## Endpoint

```http
DELETE /projects/{projectId}/members/me
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 프로젝트장은 나가기 불가 (프로젝트장 위임 후 가능)
3. `ProjectMember.LeftAt` 기록
4. 그룹 채팅방의 `ChatRoomMember`에서 제거
5. `Recruit.CurrentCount` 감소

---

## Error

| HTTP | Error Code          |
|:---- |:------------------- |
| 403  | FORBIDDEN           |
| 404  | PROJECT_NOT_FOUND   |
| 409  | LEADER_CANNOT_LEAVE |

---

# 6-7 팀원 퇴출

## Endpoint

```http
DELETE /projects/{projectId}/members/{memberId}
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name      | Description   |
|:--------- |:------------- |
| projectId | 프로젝트 ID       |
| memberId  | 퇴출할 팀원의 회원 ID |

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 프로젝트장(Leader) 여부 확인
3. 대상이 해당 프로젝트의 팀원인지 확인
4. 프로젝트장 자신은 퇴출할 수 없음
5. `ProjectMember.LeftAt` 기록
6. 그룹 채팅방의 `ChatRoomMember`에서 제거
7. `Recruit.CurrentCount` 감소
8. 퇴출된 팀원에게 알림 발송

> 인원이 비면 프로젝트장이 추가 모집(3-5)으로 `Recruit.Status = ADDITIONAL`로 전환할 수 있다. (자동 전환 아님)
> 단, 프로젝트가 이미 종료(6-4)된 이후에는 추가 모집(3-5)으로 전환할 수 없다.

---

## Error

| HTTP | Error Code               | 설명              |
|:---- |:------------------------ |:--------------- |
| 400  | CANNOT_KICK_SELF         | 프로젝트장 자신은 퇴출 불가 |
| 403  | FORBIDDEN                | 프로젝트장이 아님       |
| 404  | PROJECT_NOT_FOUND        | 프로젝트 없음         |
| 404  | PROJECT_MEMBER_NOT_FOUND | 대상이 팀원이 아님      |

---

# 6-8 프로젝트장 위임

## Endpoint

```http
PATCH /projects/{projectId}/leader
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "newLeaderId":5
}
```

---

## Response

204 No Content

---

## Business Logic

1. 현재 프로젝트장 여부 확인
2. 위임 대상이 프로젝트 팀원인지 확인
3. 프로젝트장 변경

---

## Error

| HTTP | Error Code               |
|:---- |:------------------------ |
| 403  | FORBIDDEN                |
| 404  | PROJECT_NOT_FOUND        |
| 404  | PROJECT_MEMBER_NOT_FOUND |

---

# 6-9 프로젝트 상태 조회

## Endpoint

```http
GET /projects/{projectId}/status
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "status":"IN_PROGRESS",
        "progressRate":60,
        "endDate":"2026-08-10"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 진행률 및 상태 계산
3. 결과 반환

---

## Error

| HTTP | Error Code        |
|:---- |:----------------- |
| 403  | FORBIDDEN         |
| 404  | PROJECT_NOT_FOUND |

---

# 7. Schedule

개인 일정과 팀(프로젝트) 일정을 관리하는 API이다.

## 공통 개념

### 일정 카테고리

사용자가 직접 **제목(Title)과 색상(Color)** 을 지정해 카테고리를 만들고, 일정을 생성할 때 카테고리를 선택한다.

```
카테고리 생성 (제목 + 색상)

↓

일정 생성 시 categoryId 지정

↓

캘린더에서 색상으로 구분 표시
```

### 단발 일정 / 반복 일정

단발과 반복은 **요청 정보가 달라 엔드포인트를 분리**한다.

| 구분    | Endpoint 접미사 | 필요한 정보                                    | DB (RRULE) |
|:----- |:------------ |:----------------------------------------- |:---------- |
| 단발 일정 | `/single`    | date, startTime, endTime                  | NULL       |
| 반복 일정 | `/recurring` | startDate, endDate, startTime, endTime, rrule | 값 존재       |

`rrule`은 RFC 5545 표준 문자열을 사용한다.

```text
FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20260831T235959Z
```

---

## 7-A 일정 카테고리 (Schedule Category)

# 7-1 일정 카테고리 목록 조회

## Endpoint

```http
GET /users/me/schedule-categories
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "categoryId":1,
            "title":"알바",
            "color":"#FF6B6B"
        },
        {
            "categoryId":2,
            "title":"전공 수업",
            "color":"#4D96FF"
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자의 ScheduleCategory 조회

---

# 7-2 일정 카테고리 생성

## Endpoint

```http
POST /users/me/schedule-categories
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "title":"알바",
    "color":"#FF6B6B"
}
```

### Field

| Name  | Type   | Required | Description        |
|:----- |:------ |:-------- |:------------------ |
| title | String | O        | 카테고리명 (최대 15자)     |
| color | String | O        | HEX 색상 코드 (`#RRGGBB`) |

---

## Response

### 201 Created

```json
{
    "data":{
        "categoryId":1
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 입력값 검증
3. ScheduleCategory 생성

---

## Error

| HTTP | Error Code        | 설명        |
|:---- |:----------------- |:--------- |
| 400  | VALIDATION_FAILED | 입력값 검증 실패 |

---

# 7-3 일정 카테고리 수정

## Endpoint

```http
PATCH /users/me/schedule-categories/{categoryId}
```

---

## Authentication

Bearer Access Token

---

## Request Body

카테고리 생성과 동일

---

## Response

204 No Content

---

## Business Logic

1. 본인 카테고리인지 확인
2. 제목 / 색상 수정

---

## Error

| HTTP | Error Code                  | 설명          |
|:---- |:--------------------------- |:----------- |
| 400  | VALIDATION_FAILED           | 입력값 검증 실패   |
| 403  | FORBIDDEN                   | 본인 카테고리가 아님 |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음     |

---

# 7-4 일정 카테고리 삭제

## Endpoint

```http
DELETE /users/me/schedule-categories/{categoryId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 본인 카테고리인지 확인
2. ScheduleCategory 삭제
3. 해당 카테고리를 사용하던 일정의 `SCId`는 NULL로 설정 (일정 자체는 유지)

---

## Error

| HTTP | Error Code                  | 설명          |
|:---- |:--------------------------- |:----------- |
| 403  | FORBIDDEN                   | 본인 카테고리가 아님 |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음     |

---

## 7-B 개인 일정

# 7-5 월별 일정 조회

## Endpoint

```http
GET /users/me/schedules
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name  | Type    | Required | Description |
|:----- |:------- |:-------- |:----------- |
| year  | Integer | O        | 조회 연도       |
| month | Integer | O        | 조회 월        |

---

## Response

### 200 OK

```json
{
    "data":[
        {
            "scheduleId":1,
            "title":"팀 미팅",
            "isRecurring":false,
            "date":"2026-07-10",
            "startTime":"14:00",
            "endTime":"15:00",
            "content":"킥오프 회의",
            "category":{
                "categoryId":1,
                "title":"프로젝트",
                "color":"#4D96FF"
            }
        },
        {
            "scheduleId":2,
            "title":"알바",
            "isRecurring":true,
            "startDate":"2026-07-01",
            "endDate":"2026-08-31",
            "startTime":"18:00",
            "endTime":"22:00",
            "rrule":"FREQ=WEEKLY;BYDAY=MO,WE",
            "content":null,
            "category":{
                "categoryId":2,
                "title":"알바",
                "color":"#FF6B6B"
            }
        }
    ],
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. year / month 범위에 걸치는 일정 조회
3. 반복 일정은 RRULE을 그대로 반환하며, 실제 날짜 전개(expand)는 클라이언트가 수행한다
4. 카테고리 정보(제목/색상) 포함하여 반환

---

# 7-6 개인 단발 일정 생성

## Endpoint

```http
POST /users/me/schedules/single
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "title":"팀 미팅",
    "date":"2026-07-10",
    "startTime":"14:00",
    "endTime":"15:00",
    "content":"킥오프 회의",
    "categoryId":1
}
```

### Field

| Name       | Type   | Required | Description        |
|:---------- |:------ |:-------- |:------------------ |
| title      | String | O        | 일정 제목              |
| date       | Date   | O        | 일정 날짜              |
| startTime  | Time   | O        | 시작 시간              |
| endTime    | Time   | O        | 종료 시간              |
| content    | String | X        | 상세 내용              |
| categoryId | Long   | X        | 일정 카테고리 ID         |

> 단발 일정은 DB에 `StartDate = EndDate = date`, `RRULE = NULL`로 저장된다.

---

## Response

### 201 Created

```json
{
    "data":{
        "scheduleId":1
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 입력값 검증 (startTime < endTime)
3. categoryId가 본인 카테고리인지 확인
4. MemberSchedule 생성 (`RRULE = NULL`)

---

## Error

| HTTP | Error Code                  | 설명        |
|:---- |:--------------------------- |:--------- |
| 400  | VALIDATION_FAILED           | 입력값 검증 실패 |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음   |

---

# 7-7 개인 반복 일정 생성

## Endpoint

```http
POST /users/me/schedules/recurring
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "title":"알바",
    "startDate":"2026-07-01",
    "endDate":"2026-08-31",
    "startTime":"18:00",
    "endTime":"22:00",
    "rrule":"FREQ=WEEKLY;BYDAY=MO,WE",
    "content":null,
    "categoryId":2
}
```

### Field

| Name       | Type   | Required | Description                 |
|:---------- |:------ |:-------- |:--------------------------- |
| title      | String | O        | 일정 제목                       |
| startDate  | Date   | O        | 반복 적용 시작일                   |
| endDate    | Date   | O        | 반복 적용 종료일                   |
| startTime  | Time   | O        | 하루 중 시작 시간                  |
| endTime    | Time   | O        | 하루 중 종료 시간                  |
| rrule      | String | O        | RFC 5545 반복 규칙 문자열          |
| content    | String | X        | 상세 내용                       |
| categoryId | Long   | X        | 일정 카테고리 ID                  |

---

## Response

### 201 Created

```json
{
    "data":{
        "scheduleId":2
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 입력값 검증 (startDate <= endDate, startTime < endTime)
3. RRULE 문법 검증
4. categoryId가 본인 카테고리인지 확인
5. MemberSchedule 생성 (`RRULE` 저장)

---

## Error

| HTTP | Error Code                  | 설명            |
|:---- |:--------------------------- |:------------- |
| 400  | VALIDATION_FAILED           | 입력값 검증 실패     |
| 400  | INVALID_RRULE               | RRULE 형식 오류   |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음       |

---

# 7-8 개인 일정 수정

## Endpoint

```http
PATCH /users/me/schedules/{scheduleId}
```

---

## Authentication

Bearer Access Token

---

## Request Body

단발 일정이면 7-6, 반복 일정이면 7-7의 Request Body와 동일

---

## Response

204 No Content

---

## Business Logic

1. 본인 일정인지 확인
2. 입력값 검증
3. 일정 수정

> 단발 ↔ 반복 전환은 지원하지 않는다. 종류를 바꾸려면 삭제 후 재생성한다.

---

## Error

| HTTP | Error Code                  | 설명        |
|:---- |:--------------------------- |:--------- |
| 400  | VALIDATION_FAILED           | 입력값 검증 실패 |
| 403  | FORBIDDEN                   | 본인 일정이 아님 |
| 404  | SCHEDULE_NOT_FOUND          | 일정 없음     |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음   |

---

# 7-9 개인 일정 삭제

## Endpoint

```http
DELETE /users/me/schedules/{scheduleId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 본인 일정인지 확인
2. 일정 삭제 (반복 일정은 전체 삭제)

---

## Error

| HTTP | Error Code         | 설명        |
|:---- |:------------------ |:--------- |
| 403  | FORBIDDEN          | 본인 일정이 아님 |
| 404  | SCHEDULE_NOT_FOUND | 일정 없음     |

---

## 7-C 팀 일정 (회의 일정 조율)

팀 일정은 팀원이 임의로 등록하지 않고, **회의 일정 조율(Meeting Poll)** 절차를 거쳐 확정한다.

```
1. 프로젝트장이 회의 개설
   회의명 / 소요 시간 / 탐색 기간 / 탐색 시간대 / 응답 마감(기본 12시간, 조정 가능)

2. 시스템이 30분 단위 후보 슬롯 자동 생성
   각 팀원의 개인 캘린더를 읽어 '불가'를 미리 체크해둔 상태로 제공

3. 팀원은 슬롯별 가능/불가를 표시하고 1회 제출
   캘린더가 비어 있어도 화면에서 직접 체크하면 되므로 개인 일정 등록은 강제되지 않는다

4. 전원 응답 또는 마감 시각 도달 → 자동 집계

5. 참여 가능 인원이 많은 순으로 정렬 → 프로젝트장이 최종 확정

6. PartySchedule 등록 + 전원 캘린더 반영 + 알림
```

### 설계 원칙

| 원칙            | 내용                                                    |
|:------------- |:----------------------------------------------------- |
| 캘린더는 초기값일 뿐   | 응답 화면의 체크가 유일한 진실. 캘린더 미등록자도 정상 참여 가능                 |
| 팀원 액션은 1회     | 투표 라운드를 나누지 않는다                                       |
| 미응답자는 집계 제외   | 가능/불가 어느 쪽으로도 간주하지 않고 **미응답으로 노출**                    |
| 최종 확정은 프로젝트장  | 자동 확정하지 않는다. 동점도 프로젝트장이 판단                            |

응답 마감 3시간 전, 미응답 팀원에게 리마인더 알림을 발송한다.

---

# 7-10 회의 조율 개설

## Endpoint

```http
POST /projects/{projectId}/meeting-polls
```

## Authentication

Bearer Access Token (프로젝트장만)

## Request Body

```json
{
    "title":"중간 점검 회의",
    "durationMin":60,
    "rangeStart":"2026-07-20",
    "rangeEnd":"2026-07-24",
    "dayStart":"09:00",
    "dayEnd":"22:00",
    "deadlineHours":12
}
```

### Field

| Name          | Type    | Required | Description                        |
|:------------- |:------- |:-------- |:---------------------------------- |
| title         | String  | O        | 회의명 (20자 이하)                       |
| durationMin   | Integer | O        | 소요 시간(분) — 30 / 60 / 90 / 120      |
| rangeStart    | Date    | O        | 후보 탐색 시작일                          |
| rangeEnd      | Date    | O        | 후보 탐색 종료일                          |
| dayStart      | Time    | O        | 하루 중 탐색 시작 시각                      |
| dayEnd        | Time    | O        | 하루 중 탐색 종료 시각                      |
| deadlineHours | Integer | X        | 응답 마감까지 시간 (**기본 12시간**, 프로젝트장 조정) |
| memberIds     | List<Long> | X     | 조율 참가 팀원 (미지정 시 전원, 지정 시 해당 팀원만)    |

## Response

### 201 Created

```json
{
    "data":{
        "pollId":1,
        "status":"COLLECTING",
        "deadline":"2026-07-15T03:00:00+09:00",
        "totalMembers":4,
        "respondedCount":0,
        "slotCount":125
    },
    "timestamp":"2026-07-14T15:00:00+09:00"
}
```

> `slotCount`는 탐색 기간 × 탐색 시간대를 30분 간격으로 밀어가며 `durationMin` 길이의 슬롯을 생성한 뒤,
> 참가 팀원 중 한 명이라도 개인 캘린더 일정과 겹치는 슬롯을 제외하고 남은 개수다.
> 위 예시(5일, 09:00~22:00, durationMin=60)는 캘린더 겹침이 없다면 하루 25개(09:00~21:00, 30분 간격) × 5일 = 125개다.

## Business Logic

1. 프로젝트장 여부 확인
2. `COLLECTING` 상태의 조율이 이미 있는지 확인 (프로젝트당 1개)
3. MeetingPoll 생성 (`Deadline = 현재 + deadlineHours`)
4. 참가 명단(`memberIds`) 확정. 미지정 시 팀원 전원
5. 탐색 기간 × 탐색 시간대를 **30분 단위**로 나누어 슬롯 후보 생성
6. **참가 팀원 각각의 개인 캘린더(MemberSchedule)를 조율 기간 내에서 전개하여, 그중 한 명이라도 일정이 겹치는 슬롯은 후보에서 제외**하고 남은 슬롯만 저장 (개설 시점 기준. 개설 이후 등록한 캘린더 일정은 반영되지 않는다)
7. 확정된 참가 명단을 MeetingPollMember로 등록 (`Responded = false`)
8. 그룹 채팅방 공지 + 팀원 전원 알림 발송

> 캘린더가 겹치는 슬롯은 애초에 후보로 노출되지 않으므로, 팀원들은 겹치지 않는 슬롯에 대해서만 가능/불가를 응답한다 (7-11/7-12).

## Error

| HTTP | Error Code          | 설명              |
|:---- |:------------------- |:--------------- |
| 400  | VALIDATION_FAILED   | 입력값 검증 실패       |
| 403  | FORBIDDEN           | 프로젝트장이 아님       |
| 404  | PROJECT_NOT_FOUND   | 프로젝트 없음         |
| 409  | POLL_ALREADY_ACTIVE | 진행 중인 조율이 이미 있음 |

---

# 7-11 응답 화면 조회 (슬롯 + 캘린더 프리필)

## Endpoint

```http
GET /meeting-polls/{pollId}
```

## Authentication

Bearer Access Token

## Response

### 200 OK

```json
{
    "data":{
        "pollId":1,
        "title":"중간 점검 회의",
        "status":"COLLECTING",
        "durationMin":60,
        "deadline":"2026-07-15T03:00:00+09:00",
        "totalMembers":4,
        "respondedCount":2,
        "myResponded":false,
        "slots":[
            {
                "slotId":10,
                "startAt":"2026-07-20T14:00:00+09:00",
                "endAt":"2026-07-20T15:00:00+09:00",
                "myAvailable":false,
                "prefilledByCalendar":true,
                "availableCount":3,
                "unansweredCount":1
            },
            {
                "slotId":11,
                "startAt":"2026-07-21T10:00:00+09:00",
                "endAt":"2026-07-21T11:00:00+09:00",
                "myAvailable":true,
                "prefilledByCalendar":false,
                "availableCount":4,
                "unansweredCount":0
            }
        ]
    },
    "timestamp":"2026-07-14T17:10:00+09:00"
}
```

### Field

| Name                | Type    | Description                                    |
|:------------------- |:------- |:---------------------------------------------- |
| myAvailable         | Boolean | 내 응답. **기본값 true(가능)**, 캘린더에 일정이 있으면 false로 프리필 |
| prefilledByCalendar | Boolean | 내 개인 캘린더 일정 때문에 불가로 미리 체크된 슬롯인지                |
| availableCount      | Integer | 가능이라고 응답한 인원 수                                  |
| unansweredCount     | Integer | **아직 응답하지 않은 인원 수**                            |

## Business Logic

1. 프로젝트 팀원 여부 확인
2. 슬롯 목록 조회
3. **내 개인 캘린더(MemberSchedule)를 조율 기간 내에서 전개**하여 겹치는 슬롯을 `myAvailable = false`로 프리필
4. 슬롯별 가능 인원 / 미응답 인원 집계
5. When2meet 방식으로 전체 슬롯을 격자로 노출 (후보를 자르지 않음). 확정 단계에서만 가능 인원 순으로 추천 정렬

> 캘린더는 **초기값**일 뿐이다. 팀원이 화면에서 직접 수정한 값이 최종 응답이 된다.
> 캘린더가 비어 있는 팀원도 불가 시간을 직접 체크하면 되므로 개인 일정 등록은 필수가 아니다.

## Error

| HTTP | Error Code     | 설명          |
|:---- |:-------------- |:----------- |
| 403  | FORBIDDEN      | 프로젝트 팀원이 아님 |
| 404  | POLL_NOT_FOUND | 조율 없음       |

---

# 7-12 응답 제출

## Endpoint

```http
POST /meeting-polls/{pollId}/responses
```

## Authentication

Bearer Access Token

## Request Body

```json
{
    "unavailableSlotIds":[10,15,22]
}
```

### Field

| Name               | Type       | Required | Description                       |
|:------------------ |:---------- |:-------- |:--------------------------------- |
| unavailableSlotIds | List<Long> | O        | **불가한 슬롯 ID 목록** (나머지는 모두 가능으로 처리) |

> 기본이 '가능'이므로 **불가한 슬롯만 전송**한다. 전부 가능하면 빈 배열을 보낸다.

## Response

### 200 OK

```json
{
    "data":{
        "pollId":1,
        "respondedCount":3,
        "totalMembers":4
    },
    "timestamp":"2026-07-14T18:00:00+09:00"
}
```

## Business Logic

1. 프로젝트 팀원 여부 확인
2. 조율이 `COLLECTING` 상태인지 확인
3. MeetingPollAvailability 저장 (재제출 시 갱신)
4. `MeetingPollMember.Responded = true`
5. 전원 응답 완료 시 프로젝트장에게 '확정 가능' 알림 발송

## Error

| HTTP | Error Code          | 설명            |
|:---- |:------------------- |:------------- |
| 403  | FORBIDDEN           | 프로젝트 팀원이 아님   |
| 404  | POLL_NOT_FOUND      | 조율 없음         |
| 404  | SLOT_NOT_FOUND      | 존재하지 않는 슬롯    |
| 409  | POLL_NOT_COLLECTING | 응답 수집 단계가 아님  |

---

# 7-13 최종 일정 확정

## Endpoint

```http
PATCH /meeting-polls/{pollId}/confirm
```

## Authentication

Bearer Access Token (프로젝트장만)

## Request Body

```json
{
    "slotId":11,
    "force":false
}
```

### Field

| Name   | Type    | Required | Description                              |
|:------ |:------- |:-------- |:---------------------------------------- |
| slotId | Long    | O        | 최종 확정할 슬롯                                |
| force  | Boolean | X        | 미응답자가 있는 슬롯을 확정할 때 경고를 무시하고 진행 (기본 false) |

## Response

### 200 OK

```json
{
    "data":{
        "pollId":1,
        "status":"CONFIRMED",
        "scheduleId":30
    },
    "timestamp":"2026-07-15T09:00:00+09:00"
}
```

## Business Logic

1. 프로젝트장 여부 확인
2. **미응답자가 있는 슬롯을 확정하려는 경우**, `force = false`이면 `UNANSWERED_EXISTS`를 반환하여 경고
   ("○○님은 아직 응답하지 않았습니다. 그래도 확정할까요?")
3. `PartySchedule` 생성 (단발 일정)
4. `MeetingPoll.Status = CONFIRMED`, `ScheduleId` 연결
5. **전원 캘린더에 팀 일정 반영** + 그룹 채팅방 공지 + 알림 발송

> 자동 확정하지 않는다. 동점인 경우에도 프로젝트장이 직접 선택한다.

## Error

| HTTP | Error Code          | 설명                    |
|:---- |:------------------- |:--------------------- |
| 403  | FORBIDDEN           | 프로젝트장이 아님             |
| 404  | POLL_NOT_FOUND      | 조율 없음                 |
| 404  | SLOT_NOT_FOUND      | 존재하지 않는 슬롯            |
| 409  | POLL_NOT_COLLECTING | 이미 확정되었거나 취소된 조율      |
| 409  | UNANSWERED_EXISTS   | 미응답자가 있는 슬롯 (force 필요) |

---

# 7-14 조율 취소 / 재조율

## Endpoint

```http
DELETE /meeting-polls/{pollId}
```

## Authentication

Bearer Access Token (프로젝트장만)

## Response

204 No Content

## Business Logic

1. 프로젝트장 여부 확인
2. `MeetingPoll.Status = CANCELLED`
3. 이미 확정된 조율이라면 연결된 `PartySchedule`을 삭제하고 팀원 캘린더에서도 제거
4. 팀원에게 취소 알림 발송

> 취소 후 프로젝트장은 새 조율을 개설할 수 있다. (재조율)

## Error

| HTTP | Error Code        | 설명        |
|:---- |:----------------- |:--------- |
| 403  | FORBIDDEN         | 프로젝트장이 아님 |
| 404  | POLL_NOT_FOUND    | 조율 없음     |

---

# 7-15 팀 일정 조회


## Endpoint

```http
GET /projects/{projectId}/schedules
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name  | Type    | Required | Description |
|:----- |:------- |:-------- |:----------- |
| year  | Integer | O        | 조회 연도       |
| month | Integer | O        | 조회 월        |

---

## Response

개인 일정 조회(7-5)와 동일한 구조

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. year / month 범위의 PartySchedule 조회
3. 팀원이 개인적으로 지정한 카테고리(ProjectScheduleCategory)가 있으면 해당 색상 적용

---

## Error

| HTTP | Error Code        | 설명           |
|:---- |:----------------- |:------------ |
| 403  | FORBIDDEN         | 프로젝트 참여자가 아님 |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음      |

---

# 7-16 팀 일정 직접 등록 (예외)

> 팀 일정은 원칙적으로 **회의 일정 조율(7-10~7-14)** 을 통해 확정된다.
> 본 API는 조율 없이 프로젝트장이 팀 일정을 직접 등록해야 하는 예외 상황에서만 사용한다.
> (예: 이미 확정된 외부 일정 등록)

## Endpoint

```http
POST /projects/{projectId}/schedules/single
POST /projects/{projectId}/schedules/recurring
```

---

## Authentication

Bearer Access Token (프로젝트장만 가능)

---

## Request Body

개인 일정 생성(7-6 / 7-7)과 동일 (`categoryId` 제외)

---

## Response

### 201 Created

```json
{
    "data":{
        "scheduleId":1
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 프로젝트장 여부 확인
2. 입력값 검증 (반복 일정인 경우 RRULE 문법 검증)
3. PartySchedule 생성
4. 팀원 전원의 캘린더에 반영 + 알림 발송
5. 참석/불참(7-20)은 조율로 확정한 회의와 **동일하게 적용**된다

---

## Error

| HTTP | Error Code        | 설명           |
|:---- |:----------------- |:------------ |
| 400  | VALIDATION_FAILED | 입력값 검증 실패    |
| 400  | INVALID_RRULE     | RRULE 형식 오류  |
| 403  | FORBIDDEN         | 프로젝트장이 아님    |
| 404  | PROJECT_NOT_FOUND | 프로젝트 없음      |

---

# 7-17 팀 일정 수정

## Endpoint

```http
PATCH /party-schedules/{scheduleId}
```

---

## Authentication

Bearer Access Token

---

## Request Body

개인 일정 수정(7-8)과 동일한 구조 (단발이면 date, 반복이면 startDate/endDate/rrule)이되 categoryId는 없다 (색상은 7-19로 팀원별 지정)

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. 입력값 검증
3. PartySchedule 수정

---

## Error

| HTTP | Error Code               | 설명           |
|:---- |:------------------------ |:------------ |
| 400  | VALIDATION_FAILED        | 입력값 검증 실패    |
| 403  | FORBIDDEN                | 프로젝트 참여자가 아님 |
| 404  | PARTY_SCHEDULE_NOT_FOUND | 팀 일정 없음      |

---

# 7-18 팀 일정 삭제

## Endpoint

```http
DELETE /party-schedules/{scheduleId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. PartySchedule 삭제

---

## Error

| HTTP | Error Code               | 설명           |
|:---- |:------------------------ |:------------ |
| 403  | FORBIDDEN                | 프로젝트 참여자가 아님 |
| 404  | PARTY_SCHEDULE_NOT_FOUND | 팀 일정 없음      |

---

# 7-19 프로젝트 색상(카테고리) 지정 - 개인별

## Endpoint

```http
PUT /projects/{projectId}/schedule-category
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "categoryId":3
}
```

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트 참여 여부 확인
2. categoryId가 본인 카테고리인지 확인
3. ProjectScheduleCategory 매핑 생성 또는 갱신

해당 프로젝트의 팀 일정이 내 캘린더에서 지정한 색상으로 표시된다.

프로젝트에 합류(생성 또는 추가 모집으로 참여)한 팀원은 각자 이 API로 색상을 지정한다.
지정하지 않으면 기본 색상으로 표시된다.

같은 프로젝트라도 팀원마다 다른 색상을 사용할 수 있다.

---

## Error

| HTTP | Error Code                  | 설명           |
|:---- |:--------------------------- |:------------ |
| 403  | FORBIDDEN                   | 프로젝트 참여자가 아님 |
| 404  | PROJECT_NOT_FOUND           | 프로젝트 없음      |
| 404  | SCHEDULE_CATEGORY_NOT_FOUND | 카테고리 없음      |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 7-20 회의 참석 / 불참 변경

## Endpoint

```http
PATCH /party-schedules/{scheduleId}/attendance
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "attending":false
}
```

### Field

| Name      | Type    | Required | Description            |
|:--------- |:------- |:-------- |:---------------------- |
| attending | Boolean | O        | 참석(true) / 불참(false)  |

---

## Response

204 No Content

---

## Business Logic

1. 프로젝트 팀원 여부 확인
2. `PartyScheduleAttendance`의 본인 참석 여부 갱신 (기본값 참석)
3. **그룹 채팅방에 시스템 메시지 발송** (예: "○○ 회의에 △△님이 불참합니다" / "참여합니다")

## 정책

- 회의 조율로 확정된 일정과 **프로젝트장이 직접 등록한 일정 모두** 동일하게 참석/불참을 변경할 수 있다.
- 참석/불참 변경은 **회의 삭제와 무관**하다. 불참자가 있어도 회의 일정 자체는 유지된다.
- 팀원은 회의 관리 탭에서 언제든 자신의 참석 여부를 바꿀 수 있다.

---

## Error

| HTTP | Error Code               | 설명           |
|:---- |:------------------------ |:------------ |
| 403  | FORBIDDEN                | 프로젝트 팀원이 아님  |
| 404  | PARTY_SCHEDULE_NOT_FOUND | 팀 일정 없음      |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md

---

# 8. Chat

실시간 채팅은 WebSocket을 사용한다.

REST API는 채팅방 및 채팅 내역 **조회**, **1:1 채팅방 생성**, **이미지 업로드**에 사용한다.

## WebSocket (STOMP)

| 항목 | 값 |
|:--- |:--- |
| Endpoint | `ws(s)://{host}/api/v1/ws` (SockJS) |
| 인증 | STOMP `CONNECT` 프레임의 `Authorization: Bearer {AccessToken}` 헤더 (Blacklist 포함 REST와 동일 규칙) |
| 메시지 발행(Publish) | `/pub/chatrooms/{chatRoomId}/messages` — `{ "type":"TEXT\|IMAGE", "message":"...", "imageUrl":"..." }` |
| 읽음 처리(Publish) | `/pub/chatrooms/{chatRoomId}/read` — `{ "lastReadMessageId":"..." }` (REST 8-6과 동일 로직) |
| 구독(Subscribe) | `/sub/chatrooms/{chatRoomId}` — 새 메시지 브로드캐스트 |
| 에러 알림(Subscribe) | `/user/queue/errors` — `{ "code":"FORBIDDEN", "message":"..." }` (채팅방 참여자가 아닌 경우 등) |

`senderId`는 클라이언트가 보내지 않는다. 서버가 STOMP 세션의 인증된 Principal에서만 가져온다(클라이언트가 다른 사용자를 사칭해 보낼 수 없다).

메시지가 저장되면, 발신자를 제외한 방 참여자 중 알림 설정이 `ChatNoti`(전역, 9-6) AND `notiEnabled`(방별, 8-8)가 모두 켜져 있는 사람에게 `NotificationHistory`(`type=CHAT`, `referenceType=CHATROOM`, `referenceId=chatRoomId`)를 저장한다. 소켓 연결이 끊긴 상태에서도 9-1 알림함에서 확인할 수 있다.

## 채팅방 종류

| Type   | 설명           | 생성 시점                                   |
|:------ |:------------ |:--------------------------------------- |
| DIRECT | 1:1 개인 채팅    | 상대에게 처음 메시지를 보내려 할 때 생성 (이미 있으면 기존 방 반환) |
| GROUP  | 프로젝트 팀 그룹 채팅 | 공고 작성자가 그룹 채팅을 생성할 때 (= 프로젝트 전환, 6-1 참고) |

클라이언트는 두 종류를 **별도 탭**으로 나누어 표시하며, 목록 조회 시 `type` 파라미터로 구분한다.

이미지 전송 방식

```
이미지 파일 업로드 (REST)

↓

Object Storage 저장

↓

imageUrl 반환

↓

WebSocket으로 type=IMAGE + imageUrl 전송
```

---

# 8-1 채팅방 목록

## Endpoint

```http
GET /chatrooms
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name | Type    | Required | Description                    |
|:---- |:------- |:-------- |:------------------------------ |
| type | Enum    | X        | DIRECT / GROUP (미지정 시 전체)      |
| page | Integer | X        | 기본값 0                          |
| size | Integer | X        | 기본값 10                         |

프론트의 개인 채팅 탭은 `type=DIRECT`, 그룹 채팅 탭은 `type=GROUP`으로 호출한다.

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "chatRoomId":10,
                "type":"GROUP",
                "title":"제일기획 공모전 팀",
                "projectId":1,
                "lastMessage":"내일 회의 가능하신가요?",
                "lastMessageAt":"2026-07-06T12:00:00+09:00",
                "unreadCount":2,
                "notiEnabled":true
            },
            {
                "chatRoomId":11,
                "type":"DIRECT",
                "title":"픽키",
                "projectId":null,
                "lastMessage":"지원서 잘 봤습니다.",
                "lastMessageAt":"2026-07-06T11:00:00+09:00",
                "unreadCount":0,
                "notiEnabled":true
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":2,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

> DIRECT 채팅방의 `title`은 상대방 닉네임을 사용한다. 상대가 채팅방을 나갔거나 탈퇴한 경우 '알 수 없음'으로 표시한다.
> GROUP 채팅방의 `title`은 프로젝트명을 사용한다.

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자가 속한 ChatRoom 조회
3. type 파라미터가 있으면 필터링
4. 최근 메시지 및 안 읽은 개수 계산

---

# 8-2 채팅방 상세

## Endpoint

```http
GET /chatrooms/{chatRoomId}
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "chatRoomId":1,
        "title":"제일기획 공모전 팀",
        "members":[
            {
                "memberId":10,
                "nickname":"픽키"
            }
        ],
        "projectId":1,
        "startDate":"2026-07-01",
        "endDate":"2026-08-31",
        "status":"IN_PROGRESS",
        "isLeader":true
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

> `projectId`/`startDate`/`endDate`/`status`/`isLeader`는 GROUP 채팅방일 때만 채워진다. DIRECT 채팅방 응답에는 해당 필드가 없다(null 필드 제외 정책).

---

## Business Logic

1. 채팅방 참여 여부 확인
2. 채팅방 상세 정보 조회
3. GROUP인 경우 연결된 프로젝트 정보(진행 기간, 상태, 프로젝트장 여부)를 함께 반환 — 팀원 목록은 위 `members`로 대체된다.

> **프로젝트 관리의 진입점은 그룹 채팅방이다.**
> 채팅방 상단 메뉴에서 팀원 조회, 회의 일정 잡기, 팀 일정 보기, 프로젝트 종료/연장, 프로젝트장 위임, 팀원 퇴출에 접근한다.

---

## Error

| HTTP | Error Code         |
|:---- |:------------------ |
| 403  | FORBIDDEN          |
| 404  | CHATROOM_NOT_FOUND |

---

# 8-3 이전 채팅 조회

## Endpoint

```http
GET /chatrooms/{chatRoomId}/messages
```

---

## Authentication

Bearer Access Token

---

## Query Parameter

| Name   | Type    | Required | Description       |
|:------ |:------- |:-------- |:----------------- |
| cursor | String  | X        | 이전 페이지 마지막 메시지 ID |
| size   | Integer | X        | 기본값 20            |

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "messageId":"60b9a8f1c8d2a34b5c6d7e8f",
                "senderId":10,
                "senderNickname":"픽키",
                "type":"TEXT",
                "message":"안녕하세요",
                "imageUrl":null,
                "createdAt":"2026-07-06T13:00:00+09:00"
            },
            {
                "messageId":"60b9a8f1c8d2a34b5c6d7e90",
                "senderId":10,
                "senderNickname":"픽키",
                "type":"IMAGE",
                "message":null,
                "imageUrl":"https://cdn.pickii.com/chat/2026/07/uuid.png",
                "createdAt":"2026-07-06T13:01:00+09:00"
            }
        ],
        "nextCursor":"99",
        "hasNext":true
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. 채팅방 참여 여부 확인
2. cursor 기준 이전 메시지 조회
3. 결과 반환

---

## Error

| HTTP | Error Code         |
|:---- |:------------------ |
| 403  | FORBIDDEN          |
| 404  | CHATROOM_NOT_FOUND |

---

# 8-4 채팅 이미지 업로드

## Endpoint

```http
POST /chatrooms/{chatRoomId}/images
```

---

## Authentication

Bearer Access Token

---

## Request Header

| Header       | Value               |
|:------------ |:------------------- |
| Content-Type | multipart/form-data |

---

## Request Body (multipart/form-data)

| Name  | Type | Required | Description |
|:----- |:---- |:-------- |:----------- |
| image | File | O        | 업로드할 이미지 파일 |

---

## Validation

| 항목    | Rule                                |
|:----- |:----------------------------------- |
| 형식    | jpg, jpeg, png, gif, webp           |
| 크기    | 최대 10MB                             |
| 개수    | 1회 요청당 1개                           |

---

## Response

### 201 Created

```json
{
    "data":{
        "imageUrl":"https://cdn.pickii.com/chat/2026/07/uuid.png"
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 채팅방 참여 여부 확인
3. 파일 형식 및 크기 검증
4. Object Storage에 저장 (`chat/{yyyy}/{MM}/{UUID}.{ext}`)
5. 접근 URL 반환
6. 저장된 URL을 클라이언트가 WebSocket 메시지(type=IMAGE)로 전송

> 이 API는 파일을 저장하고 URL만 반환한다.
> 실제 메시지 전송(ChatMessage 저장)은 WebSocket에서 수행한다.

---

## Error

| HTTP | Error Code         | 설명                |
|:---- |:------------------ |:----------------- |
| 400  | INVALID_FILE_TYPE  | 허용되지 않는 파일 형식     |
| 403  | FORBIDDEN          | 채팅방 참여자가 아님       |
| 404  | CHATROOM_NOT_FOUND | 채팅방 없음            |
| 413  | FILE_TOO_LARGE     | 파일 크기 초과(최대 10MB) |
| 500  | FILE_UPLOAD_FAILED | 스토리지 업로드 실패       |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 8-5 1:1 채팅방 생성

## Endpoint

```http
POST /chatrooms/direct
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "targetMemberId":15
}
```

### Field

| Name           | Type | Required | Description   |
|:-------------- |:---- |:-------- |:------------- |
| targetMemberId | Long | O        | 대화 상대 회원 ID   |

---

## Response

### 200 OK (기존 채팅방이 이미 존재하는 경우)

```json
{
    "data":{
        "chatRoomId":11,
        "type":"DIRECT",
        "isNew":false
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### 201 Created (새로 생성된 경우)

```json
{
    "data":{
        "chatRoomId":12,
        "type":"DIRECT",
        "isNew":true
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 대상 회원 존재 여부 확인
3. 본인에게 채팅 시도인지 확인 (불가)
4. 두 사용자 간 DIRECT 채팅방이 이미 존재하는지 확인
   - 존재 → 기존 chatRoomId 반환 (200)
   - 없음 → ChatRoom(`Type = DIRECT`, `ProjectId = NULL`) 생성 후 두 사용자를 ChatRoomMember로 등록 (201)

> 채팅방은 **첫 메시지를 보내려 할 때** 생성된다. 실제 메시지 전송은 WebSocket으로 수행한다.
> 동일한 두 사용자 간 DIRECT 채팅방은 항상 1개만 존재한다.
> 1:1 채팅은 **모든 회원 간 가능**하다. 프로필 조회 화면에서 바로 대화를 시작할 수 있으며,
> 지원 관계나 같은 프로젝트 소속 여부와 무관하다.

---

## Error

| HTTP | Error Code           | 설명            |
|:---- |:-------------------- |:------------- |
| 400  | CANNOT_CHAT_SELF     | 자기 자신과 채팅 불가  |
| 404  | USER_NOT_FOUND       | 대상 회원 없음      |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 8-6 채팅방 읽음 처리

## Endpoint

```http
PATCH /chatrooms/{chatRoomId}/read
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "lastReadMessageId":"60b9a8f1c8d2a34b5c6d7e90"
}
```

### Field

| Name              | Type   | Required | Description                    |
|:----------------- |:------ |:-------- |:------------------------------ |
| lastReadMessageId | String | O        | 마지막으로 읽은 메시지의 ID (MongoDB ObjectId) |

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 채팅방 참여 여부 확인
3. `ChatRoomMember.LastReadMessageId` / `LastReadAt` 갱신

## 읽음 처리 방식

읽음 여부는 메시지마다 저장하지 않고, **사용자별 읽음 커서 1개**만 저장한다.

```
unreadCount = 해당 채팅방에서 LastReadMessageId 이후 생성된 메시지 수
```

| 갱신 경로               | 사용 시점                          |
|:------------------- |:------------------------------ |
| WebSocket `read` 이벤트 | 사용자가 채팅방을 보고 있는 동안 (실시간, 기본 경로) |
| 본 REST API          | 채팅방 입장 시 / 재접속·앱 재시작 보정용        |

> 이전 채팅 조회(8-3)는 GET이므로 읽음 상태를 변경하지 않는다. (멱등성 유지)
> 읽음 갱신은 반드시 위 두 경로로만 수행한다.

---

## Error

| HTTP | Error Code         | 설명          |
|:---- |:------------------ |:----------- |
| 403  | FORBIDDEN          | 채팅방 참여자가 아님 |
| 404  | CHATROOM_NOT_FOUND | 채팅방 없음      |

---

# 8-7 채팅방 나가기

## Endpoint

```http
DELETE /chatrooms/{chatRoomId}/members/me
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

### DIRECT (1:1 채팅방)

1. 채팅방 참여 여부 확인
2. 본인의 `ChatRoomMember` row 삭제
3. 메시지 내역(MongoDB)은 그대로 유지

| 대상     | 동작                                   |
|:------ |:------------------------------------ |
| 나간 사용자 | 채팅방 목록에서 사라진다                        |
| 남은 상대방 | 채팅방은 유지되며, 상대 표시가 **'알 수 없음'** 으로 바뀐다 |
| 기존 메시지 | 삭제하지 않고 그대로 남는다                      |

> 같은 상대와 다시 대화하려면 1:1 채팅방 생성(8-5)을 호출한다. 새 채팅방이 생성되며 이전 대화 내역은 보이지 않는다.

### GROUP (프로젝트 그룹 채팅방)

**그룹 채팅방을 나가면 프로젝트에서도 탈퇴한 것으로 간주한다.**

1. 프로젝트 팀원 여부 확인
2. **프로젝트장은 나갈 수 없다.** 먼저 프로젝트장을 위임해야 한다.
3. `ChatRoomMember` 삭제
4. `ProjectMember.LeftAt` 기록 (프로젝트 탈퇴 처리)
5. `Recruit.CurrentCount` 감소
6. 팀원에게 퇴장 알림 발송

> 프로젝트 관리(팀원 확인 / 종료 / 연장 / 위임 / 퇴출 / 팀 일정)는 **그룹 채팅방을 통해 접근**한다.
> 별도의 '내 프로젝트' 목록 화면은 제공하지 않는다.

---

## Error

| HTTP | Error Code          | 설명                    |
|:---- |:------------------- |:--------------------- |
| 403  | FORBIDDEN           | 채팅방 참여자가 아님           |
| 404  | CHATROOM_NOT_FOUND  | 채팅방 없음                |
| 409  | LEADER_CANNOT_LEAVE | 프로젝트장은 위임 후에만 나갈 수 있음 |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md
- ENUM.md

---

# 8-8 채팅방 알림 설정

## Endpoint

```http
PATCH /chatrooms/{chatRoomId}/notification
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "enabled":false
}
```

### Field

| Name    | Type    | Required | Description                |
|:------- |:------- |:-------- |:-------------------------- |
| enabled | Boolean | O        | 이 채팅방의 알림 수신 여부 (기본 true) |

---

## Response

204 No Content

---

## Business Logic

1. 채팅방 참여 여부 확인
2. `ChatRoomMember.NotiEnabled` 갱신

## 정책

채팅방별 알림은 전역 채팅 알림과 함께 판단한다.

```
실제 알림 발송 = 전역 ChatNoti(ON) AND 채팅방 NotiEnabled(ON)
```

- 전역 채팅 알림(`NotificationSetting.chatNoti`)이 **OFF면 모든 채팅방 알림을 받지 않는다.**
- 전역이 ON일 때만 이 설정으로 방마다 개별 on/off 한다.

---

## Error

| HTTP | Error Code         | 설명          |
|:---- |:------------------ |:----------- |
| 403  | FORBIDDEN          | 채팅방 참여자가 아님 |
| 404  | CHATROOM_NOT_FOUND | 채팅방 없음      |

---

## Related Documents

- DB_SCHEMA.md
- ERROR_CODE.md

---

# 9. Notification

알림 조회, 읽음 처리 및 알림 설정을 관리하는 API이다.

---

# 9-1 알림 목록 조회

## Endpoint

```http
GET /notifications
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "content":[
            {
                "notificationId":1,
                "type":"APPLY",
                "title":"새로운 지원자가 있습니다.",
                "content":"픽키님이 '제일기획 공모전'에 지원했습니다.",
                "referenceType":"RECRUIT",
                "referenceId":1,
                "isRead":false,
                "sentAt":"2026-07-06T13:00:00+09:00",
                "readAt":null
            }
        ],
        "pageInfo":{
            "currentPage":0,
            "pageSize":10,
            "totalElements":5,
            "totalPages":1,
            "hasNext":false
        }
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### Field

| Name          | Type    | Description                                    |
|:------------- |:------- |:---------------------------------------------- |
| type          | Enum    | 알림 종류 (ENUM.md 7. Notification Type)           |
| referenceType | Enum    | 이동 대상 종류 (ENUM.md 17. Notification Reference Type) |
| referenceId   | Long    | 이동 대상의 PK (딥링크용)                               |
| isRead        | Boolean | 읽음 여부                                          |

알림 클릭 시 `referenceType` + `referenceId` 조합으로 화면을 이동한다.

```
referenceType = RECRUIT, referenceId = 1

↓

/recruits/1 로 이동
```

`referenceType`이 null인 경우(예: SYSTEM 알림) 이동하지 않는다.

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자 기준 Notification 조회
3. ReferenceType / ReferenceId 포함하여 반환
4. Pagination 적용

---

# 9-2 알림 읽음 처리

## Endpoint

```http
PATCH /notifications/{notificationId}/read
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 본인 알림인지 확인
2. isRead를 true로 변경

---

## Error

| HTTP | Error Code             |
|:---- |:---------------------- |
| 403  | FORBIDDEN              |
| 404  | NOTIFICATION_NOT_FOUND |

---

# 9-3 전체 읽음 처리

## Endpoint

```http
PATCH /notifications/read-all
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 로그인 사용자 기준 미읽음 Notification 전체 조회
2. isRead 일괄 true 처리

---

# 9-4 알림 삭제

## Endpoint

```http
DELETE /notifications/{notificationId}
```

---

## Authentication

Bearer Access Token

---

## Response

204 No Content

---

## Business Logic

1. 본인 알림인지 확인
2. Notification 삭제

---

## Error

| HTTP | Error Code             |
|:---- |:---------------------- |
| 403  | FORBIDDEN              |
| 404  | NOTIFICATION_NOT_FOUND |

---

# 9-5 알림 설정 조회

## Endpoint

```http
GET /users/me/notification-settings
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "chatNoti":true,
        "applicantNoti":true,
        "commentNoti":true,
        "scheduleNoti":true,
        "matchNoti":true,
        "projectNoti":true,
        "marketingNoti":false
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

### Field

| Name          | DB Column       | Description   |
|:------------- |:--------------- |:------------- |
| chatNoti      | `ChatNoti`      | 채팅 알림         |
| applicantNoti | `ApplicantNoti` | 새 지원자 알림      |
| commentNoti   | `CommentNoti`   | 내 글 댓글 알림     |
| scheduleNoti  | `ScheduleNoti`  | 팀 일정 · 회의 조율 알림 |
| matchNoti     | `MatchNoti`     | 팀 합격/매칭 결과 알림 |
| projectNoti   | `ProjectNoti`   | 프로젝트 종료 확인 · 상호평가 요청 알림 |
| marketingNoti | `MarketingNoti` | 마케팅/광고성 정보 수신 동의 |

> DB_SCHEMA.md `NotificationSetting` 테이블 기준이다.
> 회원가입 시 받은 마케팅 수신 동의(`pushNotiAgreed`)는 `marketingNoti`의 초기값으로 저장된다.

---

## Business Logic

1. Access Token 검증
2. NotificationSetting 조회

---

# 9-6 알림 설정 수정

## Endpoint

```http
PATCH /users/me/notification-settings
```

---

## Authentication

Bearer Access Token

---

## Request Body

알림 설정 조회 응답과 동일한 필드 구조

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. NotificationSetting 수정

---

# 9-7 안 읽은 알림 개수 조회

## Endpoint

```http
GET /notifications/unread-count
```

---

## Authentication

Bearer Access Token

---

## Response

### 200 OK

```json
{
    "data":{
        "unreadCount":3
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 로그인 사용자 기준 isRead = false Notification 개수 집계
3. 앱 상단 알림 뱃지(🔔) 표시에 사용

---

## Related Documents

- ERROR_CODE.md
- ENUM.md

---

# 9-8 디바이스 토큰 등록

앱이 꺼진 상태에서도 시스템 푸시(FCM)를 받기 위해, 로그인/포그라운드 진입 시 클라이언트가 발급받은 FCM 토큰을 서버에 등록한다.

## Endpoint

```http
POST /devices
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "fcmToken":"...",
    "platform":"ANDROID"
}
```

### Field

| Name     | Type   | Required | Description         |
|:-------- |:------ |:-------- |:------------------- |
| fcmToken | String | O        | 클라이언트가 발급받은 FCM 등록 토큰 |
| platform | Enum   | O        | `ANDROID` / `IOS`  |

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. `fcmToken`으로 기존 `DeviceToken` 조회
   - 있으면 소유 회원을 현재 로그인 회원으로 교체(재설치, 다른 계정 로그인 등 대비 upsert) + `UpdatedAt` 갱신
   - 없으면 신규 `DeviceToken` 저장
3. 회원 1명이 여러 기기를 등록할 수 있다 (기기별로 별도 row)

---

## Error

| HTTP | Error Code        | 설명    |
|:---- |:----------------- |:------ |
| 400  | VALIDATION_FAILED | 필드 누락 |

---

# 9-9 디바이스 토큰 삭제

로그아웃 시 해당 기기에서는 더 이상 푸시를 받지 않도록 토큰을 삭제한다.

## Endpoint

```http
DELETE /devices
```

---

## Authentication

Bearer Access Token

---

## Request Body

```json
{
    "fcmToken":"..."
}
```

### Field

| Name     | Type   | Required | Description |
|:-------- |:------ |:-------- |:----------- |
| fcmToken | String | O        | 삭제할 FCM 토큰 |

---

## Response

204 No Content

---

## Business Logic

1. Access Token 검증
2. 로그인 회원 소유의 `fcmToken`과 일치하는 `DeviceToken` 삭제
3. 이미 없거나 본인 소유가 아니어도 204를 반환한다(idempotent — 클라이언트가 삭제 성공/실패를 구분할 필요가 없음)

---

## Error

| HTTP | Error Code        | 설명    |
|:---- |:----------------- |:------ |
| 400  | VALIDATION_FAILED | 필드 누락 |

---

# 10. Member Profile

다른 회원의 프로필을 조회할 때 사용하는 API이다. 공고 작성자, 지원자, 프로젝트 팀원 등의 프로필을 확인할 때 사용한다.

---

# 10-1 회원 프로필 조회

## Endpoint

```http
GET /users/{memberId}
```

---

## Authentication

Bearer Access Token

---

## Path Variable

| Name     | Description |
|:-------- |:----------- |
| memberId | 회원 ID       |

---

## Response

```json
{
    "data":{
        "nickname":"픽키",
        "univId":1,
        "univ":"명지대학교",
        "major":"융합소프트웨어학부 데이터사이언스전공",
        "academicStatus":"ENROLLED",
        "hope":"백엔드 개발자",
        "strength":"책임감, 꼼꼼한 일정 관리",
        "aboutMe":"AI가 생성한 자기소개",
        "exp":500,
        "topic":[1,2,3],
        "skillTool":[
            {
                "techStackName":"Java",
                "level":3
            }
        ],
        "license":[
            {
                "licenseName":"정보처리기사",
                "date":"2026-05"
            }
        ],
        "experience":[
            {
                "startDate":"2026-01",
                "endDate":"2026-06",
                "title":"인턴",
                "organization":"회사",
                "description":"업무내용"
            }
        ],
        "additionalLink":[
            {
                "linkName":"Github",
                "url":"https://github.com/..."
            }
        ]
    },
    "timestamp":"2026-07-06T13:30:00+09:00"
}
```

---

## Business Logic

1. Access Token 검증
2. 대상 회원 존재 여부 확인
3. 대상 Resume 조회 후 전체 반환 (프로필은 전체 공개)

> 프로필이 없는 회원, 또는 **탈퇴한 회원**(Hard Delete)을 조회하면 `RESUME_NOT_FOUND`를 반환한다.
> 프론트는 이 경우 '알 수 없음'으로 표시한다. 공고 작성자·댓글 작성자·과거 팀원이 탈퇴한 경우가 이에 해당한다.

---

## Error

| HTTP | Error Code       | 설명                          |
|:---- |:---------------- |:--------------------------- |
| 404  | RESUME_NOT_FOUND | 프로필 없음 또는 탈퇴한 회원 ('알 수 없음') |

---

## Related Documents

- ERROR_CODE.md
- ENUM.md
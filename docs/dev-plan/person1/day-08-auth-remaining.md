# Day 8 — Auth 잔여 (1-7, 1-8, 1-9, 1-12, 1-13)

## 오늘의 목표

| 엔드포인트 | Method/URL | DoD |
|---|---|---|
| 1-7 로그아웃 | `POST /auth/logout` | 204, Refresh Token 삭제 + Access Token 블랙리스트 등록 |
| 1-8 비밀번호 재설정(비로그인) | `POST /auth/password/reset` | 204, 전체 기기 강제 로그아웃 |
| 1-9 회원 탈퇴 | `DELETE /auth/withdraw` | 204, 개인정보 삭제 but 게시글/댓글/채팅 유지 |
| 1-12 비밀번호 변경(로그인) | `POST /auth/password/code` + `PATCH /auth/password` | 204 |
| 1-13 소셜 연동 조회/해제 | `GET /users/me/social-accounts` + `DELETE /auth/social/{provider}/link` | 200 / 204 |

> **1-10(소셜 로그인), 1-11(소셜 연동 생성)은 인원2 담당입니다.** 오늘 1-13(해제)만 하면 되는데, 연동 생성 API가 아직 없어도 MySQL에 `SocialAccount` row를 직접 심어서 테스트하면 됩니다(지금까지 해온 방식 그대로).

---

## 사전 확인 사항

- `Member.changePassword(String encodedPassword)` 메서드는 이미 있습니다. 비밀번호 변경 3종(1-8, 1-9 관련 없음이지만 1-12)에서 재사용하세요.
- `SocialAccountRepository`는 이미 존재합니다.
- **"모든 기기 강제 로그아웃"을 구현하려면 Redis 패턴 삭제가 필요**합니다. `RedisKey.refreshToken(memberId, deviceId)`는 특정 기기 하나만 가리키므로, 전체 기기를 지우려면:
  ```java
  Set<String> keys = redisTemplate.keys("auth:refresh:" + memberId + ":*");
  if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
  }
  ```
  이건 1-8, 1-9, 1-12 세 곳에서 전부 재사용되니 `AuthService`에 private 헬퍼로 하나 만들어두세요.
- 결합 지점 없음. 독립적으로 진행 가능합니다.

---

## 구현 순서

### 1-7 로그아웃

```java
@Transactional
public void logout(Long memberId, String accessToken, String deviceId) {
    redisTemplate.delete(RedisKey.refreshToken(memberId, deviceId));
    long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken); // 이미 있는 메서드
    redisTemplate.opsForValue().set(RedisKey.blacklist(accessToken), String.valueOf(memberId),
            Duration.ofMillis(remainingMs));
}
```
Controller에서 `AuthController`에 추가. Access Token은 `BearerTokenUtils.resolve(httpRequest)`로 꺼내세요(이미 있는 유틸, 1-6 토큰갱신에서 쓴 것과 동일).

**중요**: 블랙리스트에 등록된 Access Token은 이후 요청에서 거부돼야 합니다. `JwtAuthenticationFilter`가 블랙리스트 체크를 이미 하고 있는지 확인하세요. 안 하고 있으면 필터에 `if (redisTemplate.hasKey(RedisKey.blacklist(token))) { 인증 실패 처리 }` 추가가 필요합니다 — **이건 공통 파일이라 수정 전 최신 main pull 필수.**

### 1-8 비밀번호 재설정 (비로그인, 이메일 인증 기반)

이미 만들어둔 `EmailVerificationPayload`/토큰 검증 패턴(1-4 회원가입 때 만든 것) 그대로 재사용합니다.

```java
@Transactional
public void resetPassword(PasswordResetRequest request) {
    if (!request.newPassword().equals(request.newPasswordConfirm())) {
        throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
    }
    EmailVerificationPayload payload = readToken(request.emailVerificationToken(), EmailVerificationPayload.class);
    if (payload.purpose() != VerificationPurpose.PW_RESET || !payload.email().equals(request.email())) {
        throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
    }
    Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));
    member.changePassword(passwordEncoder.encode(request.newPassword()));
    deleteAllRefreshTokens(member.getId()); // 전체 기기 로그아웃
    redisTemplate.delete(RedisKey.verificationToken(request.emailVerificationToken()));
}
```
> `readToken(...)`은 1-4에서 이미 만든 제네릭 헬퍼(`AuthService.readToken`)를 그대로 씁니다.

### 1-9 회원 탈퇴

```java
@Transactional
public void withdraw(Long memberId, String accessToken, WithdrawRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(...);
    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    EmailVerificationPayload payload = readToken(request.emailVerificationToken(), EmailVerificationPayload.class);
    if (payload.purpose() != VerificationPurpose.WITHDRAWAL || !payload.email().equals(member.getEmail())) {
        throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
    }
    if (!request.agreements().dataDeletionAgreed() || !request.agreements().rejoinPolicyAgreed()) {
        throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
    }
    deleteAllRefreshTokens(memberId);
    blacklistToken(accessToken);
    memberRepository.delete(member); // Hard Delete — FK가 ON DELETE SET NULL이라 작성글/댓글/채팅은 그대로 남고 작성자만 null이 됨
}
```
> **DB_Schema.md 공통 설계 원칙에 따라 Member는 Hard Delete**입니다. `Recruit.MemberId`, `Comment.MemberId` 등은 스키마상 `ON DELETE SET NULL`로 정의돼 있으니, 실제 엔티티(`@JoinColumn`)에 `nullable = true`가 걸려 있는지만 확인하면 별도 익명화 로직 없이 `delete()` 한 줄로 끝납니다. 혹시 엔티티가 `optional = false`로 걸려있으면 삭제 시 에러가 나니, 걸려있으면 오늘 `nullable = true`로 고쳐야 합니다.

### 1-12 비밀번호 변경 (로그인 상태)

**두 개의 엔드포인트**입니다.

1. `POST /auth/password/code` — 요청 바디 없음. 로그인 사용자의 이메일로 인증코드 발송.
   ```java
   public void sendPasswordChangeCode(Long memberId) {
       Member member = memberRepository.findById(memberId).orElseThrow(...);
       emailVerificationService.sendCode(member.getEmail(), VerificationPurpose.PW_RESET, "internal");
       // clientIp 자리에 "internal" 같은 더미값을 넣거나, emailVerificationService.sendCode 시그니처를 오버로드해서
       // IP 제한을 건너뛰는 버전을 하나 추가하는 것도 방법입니다 (이미 로그인된 사용자라 스팸 우려가 적음)
   }
   ```
2. `PATCH /auth/password` — 1-8과 로직 거의 동일하나 **기존 비밀번호를 요구하지 않고, 이메일도 입력받지 않음**(로그인 사용자 이메일 사용).

### 1-13 소셜 연동 조회/해제

```java
public List<SocialAccountResponse> getLinkedAccounts(Long memberId) {
    return socialAccountRepository.findByMemberId(memberId).stream()
            .map(SocialAccountResponse::from).toList();
}

@Transactional
public void unlink(Long memberId, LoginProvider provider) {
    SocialAccount account = socialAccountRepository.findByMemberIdAndProvider(memberId, provider)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LINKED_ACCOUNT));
    socialAccountRepository.delete(account);
}
```
필요하면 `SocialAccountRepository`에 `findByMemberId`, `findByMemberIdAndProvider` 쿼리 메서드 추가.

---

## 예외 케이스

| 케이스 | 코드 |
|---|---|
| 로그아웃 시 유효하지 않은 토큰 | 401 `INVALID_TOKEN` |
| 비밀번호 재설정 확인 불일치 | 400 `PASSWORD_MISMATCH` |
| 탈퇴 시 비밀번호 틀림 | 401 `INVALID_CREDENTIALS` |
| 탈퇴 시 필수 동의 누락 | 400 `REQUIRED_TERMS_NOT_AGREED` |
| 연동 안 된 provider 해제 시도 | 404 `NOT_LINKED_ACCOUNT` |

---

## 테스트 체크리스트

- [ ] 로그아웃 후 같은 Access Token으로 다른 API 호출 → 401 (블랙리스트 동작 확인)
- [ ] 로그아웃 후 Redis에서 해당 deviceId의 refresh token 키가 사라졌는지 확인
- [ ] 비번 재설정 후 기존에 로그인해둔 다른 기기의 refresh token도 전부 삭제됐는지 (여러 deviceId로 로그인 후 확인)
- [ ] 탈퇴 후 회원이 작성했던 공고/댓글은 여전히 조회되고 작성자가 "알 수 없음"(null) 처리되는지
- [ ] 탈퇴 시 비밀번호 틀리면 401, 필수 동의 안 하면 400
- [ ] 로그인 상태 비번변경: 인증코드 요청 → 코드 확인 → 비번변경까지 전체 흐름
- [ ] 소셜 연동 해제 후 조회 목록에서 사라짐

---

## 커밋/PR 가이드

- 브랜치: `feat/auth-remaining`
- 커밋: `feat: 로그아웃 API 구현 (1-7)` → `feat: 비밀번호 재설정 API 구현 (1-8)` → `feat: 회원 탈퇴 API 구현 (1-9)` → `feat: 로그인 상태 비밀번호 변경 API 구현 (1-12)` → `feat: 소셜 연동 조회/해제 API 구현 (1-13)`

---

## 막힐 수 있는 포인트

- **`JwtAuthenticationFilter`에 블랙리스트 체크가 없으면 1-7 로그아웃이 사실상 무의미**해집니다(블랙리스트에 등록만 하고 아무도 확인 안 하는 셈). 오늘 반드시 필터까지 확인하세요.
- **Member Hard Delete 시 FK 제약 에러**: 엔티티의 `@JoinColumn`에 `optional = false`가 걸려있는 곳이 있으면 삭제가 실패합니다. 에러 나면 해당 엔티티들(Recruit, Comment, Apply 등)의 Member 연관관계를 nullable로 바꿔야 하는데, 이건 Recruit/Apply를 담당한 어제 이전 작업(Day2~7)에서 이미 만든 엔티티라 수정 범위가 크지 않을 겁니다.
- **IP rate limit 재사용**: `EmailVerificationService.sendCode`를 로그인 상태 비번변경(1-12)에서 그대로 쓰면 IP 제한 로직이 다시 걸립니다. 로그인된 사용자는 스팸 위험이 낮으니 그대로 둬도 무방하지만, 신경 쓰이면 오버로드 메서드를 하나 만들어 우회하세요.

package com.pickii.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickii.domain.auth.BlacklistPayload;
import com.pickii.domain.auth.EmailVerificationPayload;
import com.pickii.domain.auth.NicknameVerificationPayload;
import com.pickii.domain.auth.RedisKey;
import com.pickii.domain.auth.RefreshTokenPayload;
import com.pickii.domain.auth.VerificationPurpose;
import com.pickii.domain.auth.VerificationType;
import com.pickii.domain.auth.client.KakaoOAuthClient;
import com.pickii.domain.auth.dto.LoginRequest;
import com.pickii.domain.auth.dto.LoginResponse;
import com.pickii.domain.auth.dto.LogoutRequest;
import com.pickii.domain.auth.dto.PasswordChangeRequest;
import com.pickii.domain.auth.dto.PasswordResetRequest;
import com.pickii.domain.auth.dto.SignupRequest;
import com.pickii.domain.auth.dto.SignupResponse;
import com.pickii.domain.auth.dto.SignupTerms;
import com.pickii.domain.auth.dto.SocialAccountResponse;
import com.pickii.domain.auth.dto.SocialLinkRequest;
import com.pickii.domain.auth.dto.SocialLinkResponse;
import com.pickii.domain.auth.dto.SocialLoginRequest;
import com.pickii.domain.auth.dto.SocialLoginResponse;
import com.pickii.domain.auth.dto.TokenRefreshRequest;
import com.pickii.domain.auth.dto.TokenRefreshResponse;
import com.pickii.domain.auth.dto.WithdrawRequest;
import com.pickii.domain.member.entity.LoginProvider;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.entity.SocialAccount;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.member.repository.SocialAccountRepository;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import com.pickii.global.security.JwtProperties;
import com.pickii.global.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 회원가입/로그인/토큰 갱신/로그아웃/비밀번호/탈퇴/소셜 연동 등 핵심 인증 처리 (API_SPEC 1-4~1-13)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final EmailVerificationService emailVerificationService;
    private final KakaoOAuthClient kakaoOAuthClient;

    /** 1-4 회원가입 */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validatePasswordMatch(request.password(), request.passwordConfirm());
        validateTerms(request.terms());

        // 두 Verification Token이 실제로 "이 이메일/닉네임"에 대해 발급된 것인지 확인한다.
        // 토큰이 존재한다고 끝이 아니라, 안에 적힌 내용이 지금 요청과 일치해야 한다.
        validateEmailToken(request.emailVerificationToken(), request.email(), VerificationPurpose.SIGNUP);
        validateNicknameToken(request.nicknameVerificationToken(), request.nickname());

        // 토큰 발급(1-1, 1-3) 이후 최대 15분 사이에 다른 사람이 같은 이메일/닉네임으로
        // 먼저 가입했을 수 있으므로, 최종 저장 직전에 한 번 더 확인한다.
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .nickname(request.nickname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();
        memberRepository.save(member);

        NotificationSetting notificationSetting = NotificationSetting.builder()
                .member(member)
                .marketingNoti(request.terms().pushNotiAgreed())
                .build();
        notificationSettingRepository.save(notificationSetting);

        redisTemplate.delete(RedisKey.verificationToken(request.emailVerificationToken()));
        redisTemplate.delete(RedisKey.verificationToken(request.nicknameVerificationToken()));

        return new SignupResponse(member.getId(), member.getEmail(), member.getNickname());
    }

    /** 1-5 로그인 */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                // 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해서 알려주지 않는다.
                // 구분해서 응답하면 "이 이메일은 가입돼 있다"는 정보가 새어나갈 수 있다.
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), request.autoLogin());

        member.updateLastLoginAt();
        saveRefreshToken(member.getId(), request.deviceId(), refreshToken, request.autoLogin());

        return new LoginResponse(member.getId(), member.getNickname(), accessToken, refreshToken);
    }

    /** 1-6 토큰 갱신 (Silent Refresh) */
    @Transactional
    public TokenRefreshResponse refreshToken(String accessToken, TokenRefreshRequest request) {
        Long memberId = extractMemberId(accessToken);
        validateRefreshTokenClaims(request.refreshToken());

        String key = RedisKey.refreshToken(memberId, request.deviceId());
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenPayload stored = readRefreshTokenPayload(json);
        if (!stored.refreshToken().equals(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        boolean autoLogin = jwtTokenProvider.isAutoLoginRefreshToken(request.refreshToken());

        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId, autoLogin);

        // Redis SET은 같은 Key를 덮어쓰므로 이 저장 자체가 RTR(기존 토큰 삭제 + 신규 저장)이다.
        saveRefreshToken(memberId, request.deviceId(), newRefreshToken, autoLogin);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    /** 1-7 로그아웃 */
    @Transactional
    public void logout(Long memberId, String accessToken, LogoutRequest request) {
        redisTemplate.delete(RedisKey.refreshToken(memberId, request.deviceId()));
        blacklistAccessToken(memberId, accessToken, "LOGOUT");
    }

    /** 1-8 비밀번호 재설정 (비로그인) */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        validatePasswordMatch(request.newPassword(), request.newPasswordConfirm());
        validateEmailToken(request.emailVerificationToken(), request.email(), VerificationPurpose.PW_RESET);

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        redisTemplate.delete(RedisKey.verificationToken(request.emailVerificationToken()));
        deleteAllRefreshTokens(member.getId());
    }

    /** 1-9 회원 탈퇴 */
    @Transactional
    public void withdraw(Long memberId, String accessToken, WithdrawRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        validateEmailToken(request.emailVerificationToken(), member.getEmail(), VerificationPurpose.WITHDRAWAL);
        if (!request.agreements().allAgreed()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        redisTemplate.delete(RedisKey.verificationToken(request.emailVerificationToken()));

        // Member Hard Delete 전에 FK로 물린 인증 관련 데이터부터 즉시 삭제한다.
        // 작성 게시글/댓글/채팅 등 다른 도메인 데이터는 유지하고 memberId만 null로 남는다(DB_SCHEMA 삭제 정책).
        socialAccountRepository.deleteAllByMemberId(memberId);
        notificationSettingRepository.deleteByMemberId(memberId);
        memberRepository.delete(member);

        deleteAllRefreshTokens(memberId);
        blacklistAccessToken(memberId, accessToken, "WITHDRAWAL");
    }

    /** 1-10 소셜 로그인 */
    @Transactional
    public SocialLoginResponse socialLogin(LoginProvider provider, SocialLoginRequest request) {
        validateSupportedProvider(provider);
        String providerId = kakaoOAuthClient.getProviderUserId(request.socialAccessToken());

        SocialAccount socialAccount = socialAccountRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LINKED_ACCOUNT));

        Member member = socialAccount.getMember();
        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), request.autoLogin());

        member.updateLastLoginAt();
        saveRefreshToken(member.getId(), request.deviceId(), refreshToken, request.autoLogin());

        return new SocialLoginResponse(accessToken, refreshToken);
    }

    /** 1-11 소셜 계정 연동 */
    @Transactional
    public SocialLinkResponse linkSocialAccount(Long memberId, LoginProvider provider, SocialLinkRequest request) {
        validateSupportedProvider(provider);

        boolean alreadyLinked = socialAccountRepository.findByProviderAndProviderId(provider, request.providerId()).isPresent()
                || socialAccountRepository.findByMemberIdAndProvider(memberId, provider).isPresent();
        if (alreadyLinked) {
            throw new BusinessException(ErrorCode.ALREADY_LINKED);
        }

        SocialAccount socialAccount = SocialAccount.builder()
                .member(memberRepository.getReferenceById(memberId))
                .provider(provider)
                .providerId(request.providerId())
                .build();
        socialAccountRepository.save(socialAccount);

        return new SocialLinkResponse("소셜 계정 연동이 완료되었습니다.");
    }

    /** 1-13 소셜 계정 연동 해제 */
    @Transactional
    public void unlinkSocialAccount(Long memberId, LoginProvider provider) {
        SocialAccount socialAccount = socialAccountRepository.findByMemberIdAndProvider(memberId, provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LINKED_ACCOUNT));
        socialAccountRepository.delete(socialAccount);
    }

    /** 1-13 소셜 계정 연동 상태 조회 (KAKAO만 지원하지만, 추후 확장될 Provider까지 목록에 함께 노출한다) */
    public List<SocialAccountResponse> getSocialAccounts(Long memberId) {
        Map<LoginProvider, SocialAccount> linkedByProvider = socialAccountRepository.findAllByMemberId(memberId).stream()
                .collect(Collectors.toMap(SocialAccount::getProvider, sa -> sa));

        return Arrays.stream(LoginProvider.values())
                .filter(provider -> provider != LoginProvider.LOCAL)
                .map(provider -> toSocialAccountResponse(provider, linkedByProvider.get(provider)))
                .toList();
    }

    private SocialAccountResponse toSocialAccountResponse(LoginProvider provider, SocialAccount socialAccount) {
        if (socialAccount == null) {
            return new SocialAccountResponse(provider, false, null);
        }
        return new SocialAccountResponse(provider, true, socialAccount.getCreatedAt().atOffset(ZoneOffset.ofHours(9)));
    }

    private void validateSupportedProvider(LoginProvider provider) {
        if (provider != LoginProvider.KAKAO) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN, "지원하지 않는 소셜 Provider입니다.");
        }
    }

    /** 1-12-1 비밀번호 변경(로그인 상태) 인증코드 발송 — 이메일을 다시 입력받지 않고 Access Token으로 식별한다. */
    public void sendPasswordChangeCode(Long memberId, String clientIp) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        emailVerificationService.sendCode(member.getEmail(), VerificationPurpose.PW_RESET, clientIp);
    }

    /** 1-12-2 비밀번호 변경(로그인 상태) */
    @Transactional
    public void changePasswordLoggedIn(Long memberId, String accessToken, PasswordChangeRequest request) {
        validatePasswordMatch(request.newPassword(), request.newPasswordConfirm());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateEmailToken(request.emailVerificationToken(), member.getEmail(), VerificationPurpose.PW_RESET);

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        redisTemplate.delete(RedisKey.verificationToken(request.emailVerificationToken()));
        deleteAllRefreshTokens(memberId);
        blacklistAccessToken(memberId, accessToken, "PASSWORD_CHANGE");
    }

    private void blacklistAccessToken(Long memberId, String accessToken, String reason) {
        long remainingMillis = jwtTokenProvider.getRemainingExpiration(accessToken);
        if (remainingMillis <= 0) {
            return;
        }
        BlacklistPayload payload = new BlacklistPayload(memberId, reason);
        redisTemplate.opsForValue().set(RedisKey.blacklist(accessToken), writeJson(payload), Duration.ofMillis(remainingMillis));
    }

    /** 회원의 모든 기기 Refresh Token을 삭제한다 (비밀번호 변경/탈퇴 시 전체 로그아웃) */
    private void deleteAllRefreshTokens(Long memberId) {
        Set<String> keys = redisTemplate.keys(RedisKey.refreshTokenPattern(memberId));
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** Silent Refresh 요청 시점엔 Access Token이 만료되어 있는 것이 정상이므로 만료는 허용하고 서명/타입만 검증한다. */
    private Long extractMemberId(String accessToken) {
        try {
            if (!jwtTokenProvider.isAccessTokenAllowExpired(accessToken)) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            return jwtTokenProvider.getMemberIdAllowExpired(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void validateRefreshTokenClaims(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private RefreshTokenPayload readRefreshTokenPayload(String json) {
        try {
            return objectMapper.readValue(json, RefreshTokenPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void saveRefreshToken(Long memberId, String deviceId, String refreshToken, boolean autoLogin) {
        Duration ttl = autoLogin
                ? Duration.ofSeconds(jwtProperties.refreshTokenValidityAutoLogin())
                : Duration.ofSeconds(jwtProperties.refreshTokenValidity());

        LocalDateTime now = LocalDateTime.now();
        RefreshTokenPayload payload = new RefreshTokenPayload(refreshToken, now, deviceId, now);
        // 동일 DeviceId로 다시 로그인하면 기존 Refresh Token은 자동으로 덮어써진다 (Redis SET은 upsert).
        redisTemplate.opsForValue().set(RedisKey.refreshToken(memberId, deviceId), writeJson(payload), ttl);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Refresh Token 직렬화에 실패했습니다.", e);
        }
    }

    private void validatePasswordMatch(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    private void validateTerms(SignupTerms terms) {
        if (!terms.allRequiredAgreed()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }

    private void validateEmailToken(String token, String expectedEmail, VerificationPurpose expectedPurpose) {
        EmailVerificationPayload payload = readToken(token, EmailVerificationPayload.class);
        boolean matches = payload.verificationType() == VerificationType.EMAIL
                && payload.purpose() == expectedPurpose
                && payload.email().equals(expectedEmail);
        if (!matches) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
    }

    private void validateNicknameToken(String token, String expectedNickname) {
        NicknameVerificationPayload payload = readToken(token, NicknameVerificationPayload.class);
        boolean matches = payload.verificationType() == VerificationType.NICKNAME
                && payload.nickname().equals(expectedNickname);
        if (!matches) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
    }

    private <T> T readToken(String token, Class<T> type) {
        String json = redisTemplate.opsForValue().get(RedisKey.verificationToken(token));
        if (json == null) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
    }
}

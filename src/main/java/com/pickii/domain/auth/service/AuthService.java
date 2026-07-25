package com.pickii.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickii.domain.auth.EmailVerificationPayload;
import com.pickii.domain.auth.NicknameVerificationPayload;
import com.pickii.domain.auth.RedisKey;
import com.pickii.domain.auth.RefreshTokenPayload;
import com.pickii.domain.auth.VerificationPurpose;
import com.pickii.domain.auth.VerificationType;
import com.pickii.domain.auth.dto.LoginRequest;
import com.pickii.domain.auth.dto.LoginResponse;
import com.pickii.domain.auth.dto.SignupRequest;
import com.pickii.domain.auth.dto.SignupResponse;
import com.pickii.domain.auth.dto.SignupTerms;
import com.pickii.domain.auth.dto.TokenRefreshRequest;
import com.pickii.domain.auth.dto.TokenRefreshResponse;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
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

/**
 * 회원가입/로그인/토큰 갱신 등 핵심 인증 처리 (API_SPEC 1-4~1-6)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /** 1-4 회원가입 */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validatePasswordMatch(request);
        validateTerms(request.terms());

        // 두 Verification Token이 실제로 "이 이메일/닉네임"에 대해 발급된 것인지 확인한다.
        // 토큰이 존재한다고 끝이 아니라, 안에 적힌 내용이 지금 요청과 일치해야 한다.
        validateEmailToken(request.emailVerificationToken(), request.email());
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

    private void validatePasswordMatch(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    private void validateTerms(SignupTerms terms) {
        if (!terms.allRequiredAgreed()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }

    private void validateEmailToken(String token, String expectedEmail) {
        EmailVerificationPayload payload = readToken(token, EmailVerificationPayload.class);
        boolean matches = payload.verificationType() == VerificationType.EMAIL
                && payload.purpose() == VerificationPurpose.SIGNUP
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

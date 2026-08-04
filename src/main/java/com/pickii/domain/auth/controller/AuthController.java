package com.pickii.domain.auth.controller;

import com.pickii.domain.auth.dto.EmailSendRequest;
import com.pickii.domain.auth.dto.EmailVerifyRequest;
import com.pickii.domain.auth.dto.EmailVerifyResponse;
import com.pickii.domain.auth.dto.LoginRequest;
import com.pickii.domain.auth.dto.LoginResponse;
import com.pickii.domain.auth.dto.LogoutRequest;
import com.pickii.domain.auth.dto.NicknameCheckResponse;
import com.pickii.domain.auth.dto.PasswordChangeRequest;
import com.pickii.domain.auth.dto.PasswordResetRequest;
import com.pickii.domain.auth.dto.SignupRequest;
import com.pickii.domain.auth.dto.SignupResponse;
import com.pickii.domain.auth.dto.SocialLinkRequest;
import com.pickii.domain.auth.dto.SocialLinkResponse;
import com.pickii.domain.auth.dto.SocialLoginRequest;
import com.pickii.domain.auth.dto.SocialLoginResponse;
import com.pickii.domain.auth.dto.TokenRefreshRequest;
import com.pickii.domain.auth.dto.TokenRefreshResponse;
import com.pickii.domain.auth.dto.WithdrawRequest;
import com.pickii.domain.auth.service.AuthService;
import com.pickii.domain.auth.service.EmailVerificationService;
import com.pickii.domain.auth.service.NicknameVerificationService;
import com.pickii.domain.member.entity.LoginProvider;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.util.BearerTokenUtils;
import com.pickii.global.common.util.IpUtils;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증(Auth) API (API_SPEC 1.)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final NicknameVerificationService nicknameVerificationService;
    private final AuthService authService;

    /** 1-1 이메일 인증번호 발송 */
    @PostMapping("/email/send")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailSendRequest request,
                                               HttpServletRequest httpRequest) {
        String clientIp = IpUtils.resolve(httpRequest);
        emailVerificationService.sendCode(request.email(), request.type(), clientIp);
        return ResponseEntity.noContent().build();
    }

    /** 1-2 이메일 인증번호 확인 */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        EmailVerifyResponse response = emailVerificationService.verifyCode(request.email(), request.code(), request.type());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-3 닉네임 중복 확인 */
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(@RequestParam String nickname) {
        NicknameCheckResponse response = nicknameVerificationService.check(nickname);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-4 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 1-5 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-6 토큰 갱신 (Silent Refresh) */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request,
                                                                           HttpServletRequest httpRequest) {
        String accessToken = BearerTokenUtils.resolve(httpRequest);
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        TokenRefreshResponse response = authService.refreshToken(accessToken, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-7 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long memberId,
                                        @Valid @RequestBody LogoutRequest request,
                                        HttpServletRequest httpRequest) {
        String accessToken = BearerTokenUtils.resolve(httpRequest);
        authService.logout(memberId, accessToken, request);
        return ResponseEntity.noContent().build();
    }

    /** 1-8 비밀번호 재설정 (비로그인) */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /** 1-9 회원 탈퇴 */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId,
                                          @Valid @RequestBody WithdrawRequest request,
                                          HttpServletRequest httpRequest) {
        String accessToken = BearerTokenUtils.resolve(httpRequest);
        authService.withdraw(memberId, accessToken, request);
        return ResponseEntity.noContent().build();
    }

    /** 1-10 소셜 로그인 */
    @PostMapping("/social/{provider}/login")
    public ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(@PathVariable LoginProvider provider,
                                                                         @Valid @RequestBody SocialLoginRequest request) {
        SocialLoginResponse response = authService.socialLogin(provider, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-11 소셜 계정 연동 */
    @PostMapping("/social/{provider}/link")
    public ResponseEntity<ApiResponse<SocialLinkResponse>> linkSocialAccount(@AuthenticationPrincipal Long memberId,
                                                                              @PathVariable LoginProvider provider,
                                                                              @Valid @RequestBody SocialLinkRequest request) {
        SocialLinkResponse response = authService.linkSocialAccount(memberId, provider, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 1-13 소셜 계정 연동 해제 */
    @DeleteMapping("/social/{provider}/link")
    public ResponseEntity<Void> unlinkSocialAccount(@AuthenticationPrincipal Long memberId,
                                                     @PathVariable LoginProvider provider) {
        authService.unlinkSocialAccount(memberId, provider);
        return ResponseEntity.noContent().build();
    }

    /** 1-12 비밀번호 변경(로그인 상태) */
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Long memberId,
                                                @Valid @RequestBody PasswordChangeRequest request,
                                                HttpServletRequest httpRequest) {
        String accessToken = BearerTokenUtils.resolve(httpRequest);
        authService.changePasswordLoggedIn(memberId, accessToken, request);
        return ResponseEntity.noContent().build();
    }
}

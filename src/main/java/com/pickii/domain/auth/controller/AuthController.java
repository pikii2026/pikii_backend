package com.pickii.domain.auth.controller;

import com.pickii.domain.auth.dto.EmailSendRequest;
import com.pickii.domain.auth.dto.EmailVerifyRequest;
import com.pickii.domain.auth.dto.EmailVerifyResponse;
import com.pickii.domain.auth.dto.LoginRequest;
import com.pickii.domain.auth.dto.LoginResponse;
import com.pickii.domain.auth.dto.NicknameCheckResponse;
import com.pickii.domain.auth.dto.SignupRequest;
import com.pickii.domain.auth.dto.SignupResponse;
import com.pickii.domain.auth.service.AuthService;
import com.pickii.domain.auth.service.EmailVerificationService;
import com.pickii.domain.auth.service.NicknameVerificationService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}

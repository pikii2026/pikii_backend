package com.pickii.domain.member.controller;

import com.pickii.domain.auth.dto.SocialAccountResponse;
import com.pickii.domain.auth.service.AuthService;
import com.pickii.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원(User) API (API_SPEC 1-13)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /** 1-13 소셜 계정 연동 상태 조회 */
    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<SocialAccountResponse>>> getSocialAccounts(@AuthenticationPrincipal Long memberId) {
        List<SocialAccountResponse> response = authService.getSocialAccounts(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

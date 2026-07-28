package com.pickii.domain.member.controller;

import com.pickii.domain.auth.dto.SocialAccountResponse;
import com.pickii.domain.auth.service.AuthService;
import com.pickii.domain.recruit.dto.RecruitScrapSummaryResponse;
import com.pickii.domain.recruit.service.RecruitService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원(User) API (API_SPEC 1-13, 3-16)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final RecruitService recruitService;

    /** 1-13 소셜 계정 연동 상태 조회 */
    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<SocialAccountResponse>>> getSocialAccounts(@AuthenticationPrincipal Long memberId) {
        List<SocialAccountResponse> response = authService.getSocialAccounts(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 3-16 스크랩한 공고 목록 조회 */
    @GetMapping("/me/scraps")
    public ResponseEntity<ApiResponse<PageResponse<RecruitScrapSummaryResponse>>> getScrappedRecruits(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<RecruitScrapSummaryResponse> response = recruitService.getScrappedRecruits(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

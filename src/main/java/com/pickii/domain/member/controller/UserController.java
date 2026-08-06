package com.pickii.domain.member.controller;

import com.pickii.domain.apply.dto.MyApplyResponse;
import com.pickii.domain.apply.service.ApplyService;
import com.pickii.domain.auth.dto.SocialAccountResponse;
import com.pickii.domain.auth.service.AuthService;
import com.pickii.domain.notification.dto.NotificationSettingRequest;
import com.pickii.domain.notification.dto.NotificationSettingResponse;
import com.pickii.domain.notification.service.NotificationSettingService;
import com.pickii.domain.recruit.dto.MyCommentResponse;
import com.pickii.domain.recruit.dto.RecruitScrapSummaryResponse;
import com.pickii.domain.recruit.dto.RecruitSummaryResponse;
import com.pickii.domain.recruit.service.RecruitService;
import com.pickii.domain.resume.dto.MemberProfileResponse;
import com.pickii.domain.resume.dto.ResumeCreateResponse;
import com.pickii.domain.resume.dto.ResumeRequest;
import com.pickii.domain.resume.dto.UserProfileResponse;
import com.pickii.domain.resume.service.ResumeService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원(User) API (API_SPEC 1-13, 3-16, 4-1~4-6, 9-5, 9-6, 10-1)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final RecruitService recruitService;
    private final ResumeService resumeService;
    private final ApplyService applyService;
    private final NotificationSettingService notificationSettingService;

    /** 1-13 소셜 계정 연동 상태 조회 */
    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<SocialAccountResponse>>> getSocialAccounts(@AuthenticationPrincipal Long memberId) {
        List<SocialAccountResponse> response = authService.getSocialAccounts(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-1 내 프로필 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@AuthenticationPrincipal Long memberId) {
        UserProfileResponse response = resumeService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-2 프로필 생성 */
    @PostMapping("/create-resume")
    public ResponseEntity<ApiResponse<ResumeCreateResponse>> createResume(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ResumeRequest request) {
        ResumeCreateResponse response = resumeService.createResume(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 4-3 프로필(이력서) 수정 */
    @PatchMapping("/me")
    public ResponseEntity<Void> updateResume(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ResumeRequest request) {
        resumeService.updateResume(memberId, request);
        return ResponseEntity.noContent().build();
    }

    /** 3-16 스크랩한 공고 목록 조회 */
    @GetMapping("/me/scraps")
    public ResponseEntity<ApiResponse<PageResponse<RecruitScrapSummaryResponse>>> getScrappedRecruits(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<RecruitScrapSummaryResponse> response = recruitService.getScrappedRecruits(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-4 지원 현황 조회 */
    @GetMapping("/me/applies")
    public ResponseEntity<ApiResponse<PageResponse<MyApplyResponse>>> getMyApplies(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MyApplyResponse> response = applyService.getMyApplies(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-5 작성한 공고 조회 */
    @GetMapping("/me/recruits")
    public ResponseEntity<ApiResponse<PageResponse<RecruitSummaryResponse>>> getMyRecruits(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<RecruitSummaryResponse> response = recruitService.getMyRecruits(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 4-6 작성한 댓글 조회 */
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<PageResponse<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MyCommentResponse> response = recruitService.getMyComments(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 9-5 알림 설정 조회 */
    @GetMapping("/me/notification-settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getNotificationSettings(
            @AuthenticationPrincipal Long memberId) {
        NotificationSettingResponse response = notificationSettingService.getSetting(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 9-6 알림 설정 수정 */
    @PatchMapping("/me/notification-settings")
    public ResponseEntity<Void> updateNotificationSettings(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody NotificationSettingRequest request) {
        notificationSettingService.updateSetting(memberId, request);
        return ResponseEntity.noContent().build();
    }

    /** 10-1 회원 프로필 조회 */
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(@PathVariable Long memberId) {
        MemberProfileResponse response = resumeService.getProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

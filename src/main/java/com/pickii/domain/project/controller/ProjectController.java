package com.pickii.domain.project.controller;

import com.pickii.domain.project.dto.LeaderDelegateRequest;
import com.pickii.domain.project.dto.ProjectDetailResponse;
import com.pickii.domain.project.dto.ProjectExtendRequest;
import com.pickii.domain.project.dto.ProjectExtendResponse;
import com.pickii.domain.project.dto.ProjectMemberResponse;
import com.pickii.domain.project.dto.ProjectStatusResponse;
import com.pickii.domain.project.service.ProjectService;
import com.pickii.global.common.response.ApiResponse;
import com.pickii.global.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 관리 API (API_SPEC 6-2 ~ 6-9)
 */
@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 6-2 프로젝트 상세 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        ProjectDetailResponse response = projectService.getDetail(memberId, projectId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 6-3 프로젝트 팀원 조회 */
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<PageResponse<ProjectMemberResponse>>> getMembers(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @PageableDefault(size = 10, sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<ProjectMemberResponse> response = projectService.getMembers(memberId, projectId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 6-4 프로젝트 종료 */
    @PatchMapping("/close")
    public ResponseEntity<Void> close(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        projectService.close(memberId, projectId);
        return ResponseEntity.noContent().build();
    }

    /** 6-5 프로젝트 진행기간 연장 */
    @PatchMapping("/extend")
    public ResponseEntity<ApiResponse<ProjectExtendResponse>> extend(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectExtendRequest request) {
        ProjectExtendResponse response = projectService.extend(memberId, projectId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /** 6-6 프로젝트 나가기 */
    @DeleteMapping("/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        projectService.leave(memberId, projectId);
        return ResponseEntity.noContent().build();
    }

    /** 6-7 팀원 퇴출 */
    @DeleteMapping("/members/{targetMemberId}")
    public ResponseEntity<Void> kick(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @PathVariable Long targetMemberId) {
        projectService.kick(memberId, projectId, targetMemberId);
        return ResponseEntity.noContent().build();
    }

    /** 6-8 프로젝트장 위임 */
    @PatchMapping("/leader")
    public ResponseEntity<Void> delegateLeader(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId,
            @Valid @RequestBody LeaderDelegateRequest request) {
        projectService.delegateLeader(memberId, projectId, request);
        return ResponseEntity.noContent().build();
    }

    /** 6-9 프로젝트 상태 조회 */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ProjectStatusResponse>> getStatus(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long projectId) {
        ProjectStatusResponse response = projectService.getStatus(memberId, projectId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}

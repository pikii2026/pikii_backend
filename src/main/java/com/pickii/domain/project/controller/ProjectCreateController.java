package com.pickii.domain.project.controller;

import com.pickii.domain.project.dto.ProjectCreateRequest;
import com.pickii.domain.project.dto.ProjectCreateResponse;
import com.pickii.domain.project.service.ProjectService;
import com.pickii.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 생성(그룹 채팅 생성) API (API_SPEC 6-1)
 */
@RestController
@RequestMapping("/recruits/{recruitId}/project")
@RequiredArgsConstructor
public class ProjectCreateController {

    private final ProjectService projectService;

    /** 6-1 프로젝트 생성 (그룹 채팅 생성) */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> createProject(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recruitId,
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectCreateResponse response = projectService.createProject(memberId, recruitId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}

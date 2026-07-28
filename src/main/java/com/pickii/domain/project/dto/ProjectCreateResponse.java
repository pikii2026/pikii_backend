package com.pickii.domain.project.dto;

import com.pickii.domain.recruit.entity.RecruitStatus;

/**
 * API_SPEC 6-1 프로젝트 생성(그룹 채팅 생성) 응답
 */
public record ProjectCreateResponse(
        Long projectId,
        Long chatRoomId,
        RecruitStatus recruitStatus,
        int memberCount
) {
}

package com.pickii.domain.resume.dto;

/**
 * API_SPEC 4-1 내 프로필 조회 응답 - 기술 스택/툴
 */
public record SkillToolItem(
        String techStackName,
        int level
) {
}

package com.pickii.domain.resume.dto;

import com.pickii.domain.member.entity.AcademicStatus;

import java.util.List;

/**
 * API_SPEC 4-1 내 프로필 조회 응답
 */
public record UserProfileResponse(
        String nickname,
        Long univId,
        String univ,
        String major,
        AcademicStatus academicStatus,
        String hope,
        String strength,
        String aboutMe,
        int exp,
        int level,
        List<Long> topic,
        List<SkillToolItem> skillTool,
        List<LicenseItem> license,
        List<ExperienceItem> experience,
        List<AdditionalLinkItem> additionalLink
) {
}

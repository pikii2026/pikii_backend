package com.pickii.domain.resume.dto;

import com.pickii.domain.member.entity.AcademicStatus;

import java.util.List;

/**
 * API_SPEC 10-1 회원 프로필 조회 응답 (4-1과 동일 구조 + contactEmail)
 */
public record MemberProfileResponse(
        String nickname,
        Long univId,
        String univ,
        String major,
        AcademicStatus academicStatus,
        String contactEmail,
        String hope,
        String strength,
        String aboutMe,
        int exp,
        List<Long> topic,
        List<SkillToolItem> skillTool,
        List<LicenseItem> license,
        List<ExperienceItem> experience,
        List<AdditionalLinkItem> additionalLink
) {
}

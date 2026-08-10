package com.pickii.domain.resume.dto;

import com.pickii.domain.member.entity.AcademicStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * API_SPEC 4-2 프로필 생성 / 4-3 프로필(이력서) 수정 요청 (동일한 Body 형태를 공유한다).
 * aboutMe는 4-3 수정에서만 사용된다.
 */
public record ResumeRequest(
        @NotNull(message = "대학교를 선택해주세요.")
        Long univId,

        @NotBlank(message = "전공을 입력해주세요.")
        @Size(min = 2, max = 50, message = "전공은 2자 이상 50자 이하로 입력해주세요.")
        String major,

        @NotNull(message = "학적 상태를 선택해주세요.")
        AcademicStatus academicStatus,

        @Size(max = 100, message = "희망 진로는 100자 이하로 입력해주세요.")
        String hope,

        @Size(max = 300, message = "장점은 300자 이하로 입력해주세요.")
        String strength,

        String aboutMe,

        List<Long> topic,

        @Valid
        List<SkillToolRequest> skillTool,

        @Valid
        List<LicenseRequest> license,

        @Valid
        List<ExperienceRequest> experience,

        @Valid
        List<AdditionalLinkRequest> additionalLink
) {
}

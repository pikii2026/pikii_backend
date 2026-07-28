package com.pickii.domain.resume.dto;

import com.pickii.domain.resume.entity.TechStack;

/**
 * API_SPEC 5-3 기술 스택 조회 응답 항목
 */
public record TechStackResponse(
        Long techStackId,
        String name,
        TechStack.Type type
) {
    public static TechStackResponse from(TechStack techStack) {
        return new TechStackResponse(techStack.getId(), techStack.getName(), techStack.getType());
    }
}

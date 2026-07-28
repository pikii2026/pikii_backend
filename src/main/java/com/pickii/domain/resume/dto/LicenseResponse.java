package com.pickii.domain.resume.dto;

import com.pickii.domain.resume.entity.License;

/**
 * API_SPEC 5-4 자격증 조회 응답 항목
 */
public record LicenseResponse(
        Long licenseId,
        String name
) {
    public static LicenseResponse from(License license) {
        return new LicenseResponse(license.getId(), license.getName());
    }
}

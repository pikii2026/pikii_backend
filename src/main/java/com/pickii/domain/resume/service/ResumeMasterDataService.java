package com.pickii.domain.resume.service;

import com.pickii.domain.resume.dto.LicenseResponse;
import com.pickii.domain.resume.dto.LinkCategoryResponse;
import com.pickii.domain.resume.dto.TechStackResponse;
import com.pickii.domain.resume.repository.LicenseRepository;
import com.pickii.domain.resume.repository.LinkCategoryRepository;
import com.pickii.domain.resume.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기술 스택 조회 (API_SPEC 5-3), 자격증 조회 (API_SPEC 5-4), 링크 카테고리 조회 (API_SPEC 5-5)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeMasterDataService {

    private final TechStackRepository techStackRepository;
    private final LicenseRepository licenseRepository;
    private final LinkCategoryRepository linkCategoryRepository;

    public List<TechStackResponse> getTechStacks() {
        return techStackRepository.findAll().stream()
                .map(TechStackResponse::from)
                .toList();
    }

    public List<LicenseResponse> getLicenses() {
        return licenseRepository.findAll().stream()
                .map(LicenseResponse::from)
                .toList();
    }

    public List<LinkCategoryResponse> getLinkCategories() {
        return linkCategoryRepository.findAll().stream()
                .map(LinkCategoryResponse::from)
                .toList();
    }
}

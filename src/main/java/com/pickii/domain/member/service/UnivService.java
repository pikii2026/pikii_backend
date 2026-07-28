package com.pickii.domain.member.service;

import com.pickii.domain.member.dto.UnivResponse;
import com.pickii.domain.member.repository.UnivRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 대학교 목록 조회 (API_SPEC 5-8)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnivService {

    private final UnivRepository univRepository;

    public List<UnivResponse> getUnivs(String keyword) {
        var univs = StringUtils.hasText(keyword)
                ? univRepository.findByNameContaining(keyword.trim())
                : univRepository.findAll();
        return univs.stream()
                .map(UnivResponse::from)
                .toList();
    }
}

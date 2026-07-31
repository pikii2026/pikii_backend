package com.pickii.domain.member.service;

import com.pickii.domain.member.dto.UnivResponse;
import com.pickii.domain.member.repository.UnivRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 대학교 목록 조회 (API_SPEC 5-8)
 * 마스터 데이터는 거의 변경되지 않으므로 캐싱한다 (API_SPEC 5장, 검색어별로 캐시됨).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnivService {

    private final UnivRepository univRepository;

    @Cacheable(value = "univs", key = "#keyword != null ? #keyword : ''")
    public List<UnivResponse> getUnivs(String keyword) {
        var univs = StringUtils.hasText(keyword)
                ? univRepository.findByNameContaining(keyword.trim())
                : univRepository.findAll();
        return univs.stream()
                .map(UnivResponse::from)
                .toList();
    }
}

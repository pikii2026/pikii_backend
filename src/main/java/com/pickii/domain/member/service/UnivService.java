package com.pickii.domain.member.service;

import com.pickii.domain.member.dto.UnivResponse;
import com.pickii.domain.member.entity.Univ;
import com.pickii.domain.member.repository.UnivRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 대학교 목록 조회 (API_SPEC 5-8)
 * 마스터 데이터는 거의 변경되지 않으므로 캐싱한다.
 *
 * <p>검색어(keyword)는 자유 텍스트라 캐시 키로 쓰면 spring.cache.type=simple(무제한
 * ConcurrentHashMap) 특성상 서로 다른 검색어 수만큼 캐시가 영구적으로 쌓인다.
 * 그래서 캐싱은 "전체 목록"(키 없는 단일 엔트리) 조회에만 적용하고,
 * 검색어가 있는 경우는 매번 조회한다 (대학교 테이블 자체가 작아 비용이 낮음).</p>
 *
 * <p>{@code @Cacheable}은 Spring 프록시를 거쳐야 동작하므로, 같은 클래스 안에서
 * {@code this.getAllUnivsCached()}로 직접 호출하면 캐싱이 무시된다(self-invocation).
 * 그래서 자기 자신을 지연 주입(self)해 프록시를 통해 호출한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class UnivService {

    private final UnivRepository univRepository;
    private final UnivService self;

    public UnivService(UnivRepository univRepository, @Lazy UnivService self) {
        this.univRepository = univRepository;
        this.self = self;
    }

    public List<UnivResponse> getUnivs(String keyword) {
        var univs = StringUtils.hasText(keyword)
                ? univRepository.findByNameContaining(keyword.trim())
                : self.getAllUnivsCached();
        return univs.stream()
                .map(UnivResponse::from)
                .toList();
    }

    @Cacheable("univs")
    public List<Univ> getAllUnivsCached() {
        return univRepository.findAll();
    }
}

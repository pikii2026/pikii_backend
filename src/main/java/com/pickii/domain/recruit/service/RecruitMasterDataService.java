package com.pickii.domain.recruit.service;

import com.pickii.domain.recruit.dto.CategoryResponse;
import com.pickii.domain.recruit.dto.TopicResponse;
import com.pickii.domain.recruit.repository.CategoryRepository;
import com.pickii.domain.recruit.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 조회 (API_SPEC 5-1), 주제 조회 (API_SPEC 5-2)
 * 마스터 데이터는 거의 변경되지 않으므로 캐싱한다 (API_SPEC 5장). 변경은 DB 직접 조작으로만
 * 이뤄지므로(별도 관리 API 없음) TTL/무효화 없이 앱 재기동 시까지 캐시를 유지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitMasterDataService {

    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;

    @Cacheable("categories")
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Cacheable("topics")
    public List<TopicResponse> getTopics() {
        return topicRepository.findAll().stream()
                .map(TopicResponse::from)
                .toList();
    }
}

package com.pickii.domain.recruit.service;

import com.pickii.domain.recruit.dto.CategoryResponse;
import com.pickii.domain.recruit.dto.TopicResponse;
import com.pickii.domain.recruit.repository.CategoryRepository;
import com.pickii.domain.recruit.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 조회 (API_SPEC 5-1), 주제 조회 (API_SPEC 5-2)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitMasterDataService {

    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<TopicResponse> getTopics() {
        return topicRepository.findAll().stream()
                .map(TopicResponse::from)
                .toList();
    }
}

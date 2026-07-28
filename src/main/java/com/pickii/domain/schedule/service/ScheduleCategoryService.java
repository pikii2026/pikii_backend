package com.pickii.domain.schedule.service;

import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.schedule.dto.ScheduleCategoryCreateResponse;
import com.pickii.domain.schedule.dto.ScheduleCategoryRequest;
import com.pickii.domain.schedule.dto.ScheduleCategoryResponse;
import com.pickii.domain.schedule.entity.ScheduleCategory;
import com.pickii.domain.schedule.repository.MemberScheduleRepository;
import com.pickii.domain.schedule.repository.ScheduleCategoryRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 일정 카테고리 목록 조회 (API_SPEC 7-1), 생성 (7-2), 수정 (7-3), 삭제 (7-4)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCategoryService {

    private final ScheduleCategoryRepository scheduleCategoryRepository;
    private final MemberScheduleRepository memberScheduleRepository;
    private final MemberRepository memberRepository;

    /** 7-1 일정 카테고리 목록 조회 */
    public List<ScheduleCategoryResponse> getCategories(Long memberId) {
        return scheduleCategoryRepository.findAllByMemberId(memberId).stream()
                .map(ScheduleCategoryResponse::from)
                .toList();
    }

    /** 7-2 일정 카테고리 생성 */
    @Transactional
    public ScheduleCategoryCreateResponse create(Long memberId, ScheduleCategoryRequest request) {
        ScheduleCategory category = ScheduleCategory.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .color(request.color())
                .build();
        scheduleCategoryRepository.save(category);
        return new ScheduleCategoryCreateResponse(category.getId());
    }

    /** 7-3 일정 카테고리 수정 */
    @Transactional
    public void update(Long memberId, Long categoryId, ScheduleCategoryRequest request) {
        ScheduleCategory category = getOwnedCategory(memberId, categoryId);
        category.update(request.title(), request.color());
    }

    /** 7-4 일정 카테고리 삭제 */
    @Transactional
    public void delete(Long memberId, Long categoryId) {
        ScheduleCategory category = getOwnedCategory(memberId, categoryId);
        memberScheduleRepository.clearCategory(categoryId);
        scheduleCategoryRepository.delete(category);
    }

    private ScheduleCategory getOwnedCategory(Long memberId, Long categoryId) {
        ScheduleCategory category = scheduleCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_CATEGORY_NOT_FOUND));
        if (!category.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return category;
    }
}

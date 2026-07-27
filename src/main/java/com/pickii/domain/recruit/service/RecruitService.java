package com.pickii.domain.recruit.service;

import com.pickii.domain.member.repository.MemberUnivRepository;
import com.pickii.domain.recruit.dto.RecruitSummaryResponse;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.repository.RecruitRepository;
import com.pickii.domain.recruit.repository.RecruitSpecification;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 메인(Home) 공고 검색/목록 조회 (API_SPEC 2-1)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitService {

    private static final int KEYWORD_MIN_LENGTH = 2;

    private final RecruitRepository recruitRepository;
    private final MemberUnivRepository memberUnivRepository;

    public PageResponse<RecruitSummaryResponse> searchRecruits(String keyword, Boolean onCampus,
                                                                List<Long> categoryIds, List<Long> topicIds,
                                                                Long viewerMemberId, Pageable pageable) {
        Specification<Recruit> spec = RecruitSpecification.notDeleted()
                .and(RecruitSpecification.fetchMember());

        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            if (trimmed.length() < KEYWORD_MIN_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "검색어는 %d자 이상 입력해주세요.".formatted(KEYWORD_MIN_LENGTH));
            }
            spec = spec.and(RecruitSpecification.titleContains(trimmed));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and(RecruitSpecification.categoryIn(categoryIds));
        }
        if (topicIds != null && !topicIds.isEmpty()) {
            spec = spec.and(RecruitSpecification.topicIn(topicIds));
        }
        spec = spec.and(onCampusSpecification(onCampus, viewerMemberId));

        Page<Recruit> page = recruitRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::toSummary);
    }

    private Specification<Recruit> onCampusSpecification(Boolean onCampus, Long viewerMemberId) {
        if (Boolean.FALSE.equals(onCampus)) {
            return RecruitSpecification.offCampusOnly();
        }
        Long viewerUnivId = resolveUnivId(viewerMemberId);
        if (Boolean.TRUE.equals(onCampus)) {
            // 학교 정보가 없는 회원/비회원은 onCampus=true 필터 자체를 사용할 수 없다.
            if (viewerUnivId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "학교 정보가 없어 교내 공고를 조회할 수 없습니다.");
            }
            return RecruitSpecification.onCampusForUniv(viewerUnivId);
        }
        return RecruitSpecification.visibleGivenUniv(viewerUnivId);
    }

    private Long resolveUnivId(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberUnivRepository.findById(memberId)
                .map(memberUniv -> memberUniv.getUniv().getId())
                .orElse(null);
    }

    private RecruitSummaryResponse toSummary(Recruit recruit) {
        String authorNickname = recruit.getMember() == null ? "알 수 없음" : recruit.getMember().getNickname();
        return new RecruitSummaryResponse(
                recruit.getId(),
                recruit.getTitle(),
                authorNickname,
                recruit.getTargetCount(),
                recruit.getAvailableSlots(),
                recruit.getStatus(),
                recruit.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}

package com.pickii.domain.apply.service;

import com.pickii.domain.apply.dto.ApplyAiDraftRequest;
import com.pickii.domain.apply.dto.ApplyAiDraftResponse;
import com.pickii.domain.apply.dto.ApplyCreateRequest;
import com.pickii.domain.apply.entity.Apply;
import com.pickii.domain.apply.entity.ApplyKeywordMap;
import com.pickii.domain.apply.repository.ApplyKeywordMapRepository;
import com.pickii.domain.apply.repository.ApplyKeywordRepository;
import com.pickii.domain.apply.repository.ApplyRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.repository.RecruitRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AI 지원서 초안 생성 (API_SPEC 3-10), 공고 지원하기 (API_SPEC 3-11), 지원 취소 (API_SPEC 3-13)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyService {

    private final ApplyRepository applyRepository;
    private final ApplyKeywordRepository applyKeywordRepository;
    private final ApplyKeywordMapRepository applyKeywordMapRepository;
    private final RecruitRepository recruitRepository;
    private final MemberRepository memberRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    /**
     * 3-10 AI 지원서 초안 생성
     *
     * <p>TODO: 실제 AI 서버 연동 전까지는 입력값을 가공해 돌려주는 목업으로 구현한다.</p>
     */
    public ApplyAiDraftResponse generateAiDraft(ApplyAiDraftRequest request) {
        return new ApplyAiDraftResponse("AI가 다듬은: " + request.message());
    }

    /** 3-11 공고 지원하기 */
    @Transactional
    public void apply(Long memberId, Long recruitId, ApplyCreateRequest request) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));

        if (!recruit.getStatus().isApplicable()) {
            throw new BusinessException(ErrorCode.RECRUIT_CLOSED);
        }
        if (applyRepository.existsByRecruitIdAndMemberId(recruitId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        Set<Long> keywordIds = request.keywordIds() == null ? Set.of() : new LinkedHashSet<>(request.keywordIds());
        if (!keywordIds.isEmpty() && applyKeywordRepository.countByIdIn(keywordIds) != keywordIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 키워드가 포함되어 있습니다.");
        }

        Member applicant = memberRepository.getReferenceById(memberId);

        Apply apply = Apply.builder()
                .recruit(recruit)
                .member(applicant)
                .message(request.message())
                .build();
        applyRepository.save(apply);

        keywordIds.forEach(keywordId -> applyKeywordMapRepository.save(new ApplyKeywordMap(apply.getId(), keywordId)));

        notifyRecruitAuthor(recruit, applicant);
    }

    /** 3-13 지원 취소 */
    @Transactional
    public void cancelApply(Long memberId, Long applyId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND));
        if (apply.getMember() == null || !apply.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!apply.isWaiting()) {
            throw new BusinessException(ErrorCode.APPLY_NOT_WAITING);
        }

        applyKeywordMapRepository.deleteAllByApplyId(applyId);
        applyRepository.delete(apply);
    }

    private void notifyRecruitAuthor(Recruit recruit, Member applicant) {
        Member author = recruit.getMember();
        if (author == null) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(author.getId()).orElse(null);
        if (setting == null || !setting.isApplicantNoti()) {
            return;
        }
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(author)
                .title("새로운 지원자가 있습니다.")
                .content(applicant.getNickname() + "님이 '" + recruit.getTitle() + "'에 지원했습니다.")
                .type(NotificationType.APPLY)
                .referenceType(NotificationReferenceType.RECRUIT)
                .referenceId(recruit.getId())
                .build());
    }
}

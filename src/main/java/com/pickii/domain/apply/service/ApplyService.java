package com.pickii.domain.apply.service;

import com.pickii.domain.apply.dto.ApplicantResponse;
import com.pickii.domain.apply.dto.ApplyAiDraftRequest;
import com.pickii.domain.apply.dto.ApplyAiDraftResponse;
import com.pickii.domain.apply.dto.ApplyCreateRequest;
import com.pickii.domain.apply.dto.ApplyKeywordCategoryResponse;
import com.pickii.domain.apply.dto.ApplyStatusUpdateRequest;
import com.pickii.domain.apply.dto.MyApplyResponse;
import com.pickii.domain.apply.entity.Apply;
import com.pickii.domain.apply.entity.ApplyKeyword;
import com.pickii.domain.apply.entity.ApplyKeywordCategory;
import com.pickii.domain.apply.entity.ApplyKeywordMap;
import com.pickii.domain.apply.entity.ApplyStatus;
import com.pickii.domain.apply.repository.ApplyKeywordCategoryRepository;
import com.pickii.domain.apply.repository.ApplyKeywordMapRepository;
import com.pickii.domain.apply.repository.ApplyKeywordRepository;
import com.pickii.domain.apply.repository.ApplyRepository;
import com.pickii.domain.chat.entity.ChatRoom;
import com.pickii.domain.chat.entity.ChatRoomMember;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.repository.RecruitRepository;
import com.pickii.global.ai.GeminiClient;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 지원서 초안 생성 (API_SPEC 3-10), 공고 지원하기 (API_SPEC 3-11), 지원 취소 (API_SPEC 3-13),
 * 지원 현황 조회 (API_SPEC 4-4), 지원자 목록 조회 (API_SPEC 4-7), 지원자 수락/거절 (API_SPEC 4-8)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyService {

    private final ApplyRepository applyRepository;
    private final ApplyKeywordRepository applyKeywordRepository;
    private final ApplyKeywordCategoryRepository applyKeywordCategoryRepository;
    private final ApplyKeywordMapRepository applyKeywordMapRepository;
    private final RecruitRepository recruitRepository;
    private final MemberRepository memberRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final GeminiClient geminiClient;

    /** 3-10 AI 지원서 초안 생성 (Gemini) */
    public ApplyAiDraftResponse generateAiDraft(Long recruitId, ApplyAiDraftRequest request) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));

        String prompt = """
                너는 대학생 공모전/스터디/프로젝트 팀원 모집 공고에 지원하는 지원자의 메시지를 다듬어주는 도우미다.
                아래 공고 내용을 참고하여, 지원자가 작성한 원본 메시지를 공고 맥락에 맞게 더 정중하고 설득력 있게 다듬어라.
                없는 사실을 지어내지 말고, 결과는 다듬어진 지원 메시지 본문만 출력하라 (설명, 따옴표, 접두사 없이).

                [공고 정보]
                제목: %s
                간단소개: %s
                상세내용: %s

                [지원자 원본 메시지]
                %s
                """.formatted(recruit.getTitle(), recruit.getSimpleDesc(), recruit.getContent(), request.message());

        String convertedText = geminiClient.generateText(prompt);
        return new ApplyAiDraftResponse(convertedText.trim());
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

    /** 5-7 지원 키워드 조회 (카테고리별 Nested) */
    public List<ApplyKeywordCategoryResponse> getApplyKeywords() {
        Map<Long, List<ApplyKeyword>> keywordsByCategoryId = applyKeywordRepository.findAll().stream()
                .collect(Collectors.groupingBy(keyword -> keyword.getCategory().getId()));

        return applyKeywordCategoryRepository.findAll().stream()
                .map(category -> toApplyKeywordCategoryResponse(category, keywordsByCategoryId))
                .toList();
    }

    private ApplyKeywordCategoryResponse toApplyKeywordCategoryResponse(
            ApplyKeywordCategory category, Map<Long, List<ApplyKeyword>> keywordsByCategoryId) {
        List<ApplyKeywordCategoryResponse.KeywordItem> items = keywordsByCategoryId
                .getOrDefault(category.getId(), List.of()).stream()
                .map(ApplyKeywordCategoryResponse.KeywordItem::from)
                .toList();
        return new ApplyKeywordCategoryResponse(category.getId(), category.getName(), items);
    }

    /** 4-4 지원 현황 조회 */
    public PageResponse<MyApplyResponse> getMyApplies(Long memberId, Pageable pageable) {
        Page<Apply> applies = applyRepository.findByMemberId(memberId, pageable);
        return PageResponse.from(applies, this::toMyApplyResponse);
    }

    private MyApplyResponse toMyApplyResponse(Apply apply) {
        Recruit recruit = apply.getRecruit();
        return new MyApplyResponse(
                apply.getId(),
                recruit.getId(),
                recruit.getTitle(),
                recruit.getStatus(),
                apply.getStatus(),
                apply.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    /** 4-7 지원자 목록 조회 */
    public PageResponse<ApplicantResponse> getApplicants(Long memberId, Long recruitId, Pageable pageable) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Page<Apply> applies = applyRepository.findByRecruitId(recruitId, pageable);
        return PageResponse.from(applies, this::toApplicantResponse);
    }

    private ApplicantResponse toApplicantResponse(Apply apply) {
        Member applicant = apply.getMember();
        return new ApplicantResponse(
                apply.getId(),
                applicant == null ? null : applicant.getId(),
                applicant == null ? "알 수 없음" : applicant.getNickname(),
                apply.getMessage(),
                apply.getStatus(),
                apply.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    /** 4-8 지원자 수락/거절 */
    @Transactional
    public void updateStatus(Long memberId, Long applyId, ApplyStatusUpdateRequest request) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND));
        Recruit recruit = apply.getRecruit();
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!apply.isWaiting()) {
            throw new BusinessException(ErrorCode.APPLY_NOT_WAITING, "이미 처리된 지원건은 상태를 변경할 수 없습니다.");
        }

        if (request.status() == ApplyStatus.ACCEPTED) {
            if (recruit.getAvailableSlots() <= 0) {
                throw new BusinessException(ErrorCode.RECRUIT_FULL);
            }
            apply.accept();
            recruit.increaseCurrentCount();
            joinExistingProjectIfPresent(recruit, apply.getMember());
            notifyApplicant(apply.getMember(), recruit, true);
        } else if (request.status() == ApplyStatus.REJECTED) {
            apply.reject();
            notifyApplicant(apply.getMember(), recruit, false);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "ACCEPTED 또는 REJECTED만 입력 가능합니다.");
        }
    }

    /** 이미 그룹 채팅(Project)이 생성된 공고라면, 수락 즉시 팀원으로 등록하고 그룹 채팅방에 자동 초대한다. */
    private void joinExistingProjectIfPresent(Recruit recruit, Member acceptedMember) {
        if (acceptedMember == null) {
            return;
        }
        projectRepository.findByRecruitId(recruit.getId()).ifPresent(project -> {
            projectMemberRepository.save(new ProjectMember(project, acceptedMember, false));
            ChatRoom groupChatRoom = chatRoomRepository.findByProjectId(project.getId())
                    .orElseThrow(() -> new IllegalStateException("GROUP 채팅방이 없는 프로젝트입니다."));
            chatRoomMemberRepository.save(new ChatRoomMember(groupChatRoom, acceptedMember));
        });
    }

    private void notifyApplicant(Member applicant, Recruit recruit, boolean accepted) {
        if (applicant == null) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(applicant.getId()).orElse(null);
        if (setting == null || !setting.isMatchNoti()) {
            return;
        }
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(applicant)
                .title(accepted ? "지원이 수락되었습니다." : "지원이 거절되었습니다.")
                .content("'" + recruit.getTitle() + "' 공고에 대한 지원이 " + (accepted ? "수락" : "거절") + "되었습니다.")
                .type(accepted ? NotificationType.ACCEPT : NotificationType.REJECT)
                .referenceType(NotificationReferenceType.RECRUIT)
                .referenceId(recruit.getId())
                .build());
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

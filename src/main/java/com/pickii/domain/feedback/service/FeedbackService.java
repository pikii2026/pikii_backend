package com.pickii.domain.feedback.service;

import com.pickii.domain.feedback.dto.AiFeedbackDetailResponse;
import com.pickii.domain.feedback.dto.EvaluationPeriodResponse;
import com.pickii.domain.feedback.dto.FeedbackCreateRequest;
import com.pickii.domain.feedback.dto.FeedbackListResponse;
import com.pickii.domain.feedback.dto.FeedbackTargetResponse;
import com.pickii.domain.feedback.dto.TargetMemberResponse;
import com.pickii.domain.feedback.dto.KeywordResponse;
import com.pickii.domain.feedback.entity.AIFeedback;
import com.pickii.domain.feedback.entity.Feedback;
import com.pickii.domain.feedback.repository.AIFeedbackRepository;
import com.pickii.domain.feedback.repository.FeedbackRepository;
import com.pickii.domain.feedback.repository.KeywordRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.entity.ProjectStatus;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 평가 대상 팀원 조회 (API_SPEC 4-9), 상호평가 작성 (API_SPEC 4-10),
 * AI 피드백 목록 조회 (API_SPEC 4-11), AI 피드백 상세 조회 (API_SPEC 4-12)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    /** 평가 기간: 프로젝트 종료(END) 시점으로부터 3일 */
    private static final int EVALUATION_PERIOD_DAYS = 3;

    private final FeedbackRepository feedbackRepository;
    private final AIFeedbackRepository aiFeedbackRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final KeywordRepository keywordRepository;

    /** 5-6 피드백 키워드 조회 */
    public List<KeywordResponse> getKeywords() {
        return keywordRepository.findAll().stream()
                .map(KeywordResponse::from)
                .toList();
    }

    /** 4-9 평가 대상 팀원 조회 */
    public FeedbackTargetResponse getEvaluationTargets(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        requireActiveMember(project.getId(), memberId);
        requireEnded(project);
        LocalDateTime deadline = evaluationDeadline(project);
        requireWithinDeadline(deadline);

        List<TargetMemberResponse> targets = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId())
                .stream()
                .filter(pm -> !pm.getMember().getId().equals(memberId))
                .map(pm -> new TargetMemberResponse(
                        pm.getMember().getId(),
                        pm.getMember().getNickname(),
                        feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(
                                project.getId(), memberId, pm.getMember().getId())
                ))
                .toList();

        return new FeedbackTargetResponse(project.getId(), deadline.atOffset(ZoneOffset.ofHours(9)), targets);
    }

    /** 4-10 상호평가 작성 */
    @Transactional
    public void write(Long memberId, FeedbackCreateRequest request) {
        Project project = getProject(request.projectId());
        requireActiveMember(project.getId(), memberId);
        requireEnded(project);
        requireWithinDeadline(evaluationDeadline(project));

        if (request.revieweeId().equals(memberId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "본인은 평가 대상이 될 수 없습니다.");
        }
        if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(project.getId(), request.revieweeId())) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }
        if (feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(project.getId(), memberId, request.revieweeId())) {
            throw new BusinessException(ErrorCode.ALREADY_EVALUATED);
        }

        Member reviewer = memberRepository.getReferenceById(memberId);
        Member reviewee = memberRepository.getReferenceById(request.revieweeId());
        Feedback feedback = Feedback.builder()
                .project(project)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .commitScore(request.scores().responsibility())
                .commScore(request.scores().communication())
                .deadlineScore(request.scores().deadline())
                .cooperateScore(request.scores().cooperation())
                .contributeScore(request.scores().contribution())
                .strengthText(request.strength())
                .weaknessText(request.weakness())
                .build();
        feedbackRepository.save(feedback);

        checkAndGenerateAiFeedback(project, request.revieweeId());
    }

    /** 4-11 AI 피드백 목록 조회 */
    public PageResponse<FeedbackListResponse> getMyFeedbackList(Long memberId, Pageable pageable) {
        Page<ProjectMember> page = projectMemberRepository.findEndedProjectsByMemberId(memberId, pageable);
        return PageResponse.from(page, pm -> toFeedbackListItem(pm.getProject(), memberId));
    }

    private FeedbackListResponse toFeedbackListItem(Project project, Long memberId) {
        int teamSize = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId()).size();
        long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), memberId);
        int requiredCount = requiredEvaluatorCount(teamSize);

        LocalDate start = project.getEndedAt().toLocalDate();
        LocalDate end = start.plusDays(EVALUATION_PERIOD_DAYS);
        int remainingDays = (int) Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), end));
        boolean available = aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), memberId).isPresent();

        return new FeedbackListResponse(
                project.getId(),
                project.getName(),
                new EvaluationPeriodResponse(start, end),
                remainingDays,
                teamSize,
                (int) evaluatedCount,
                requiredCount,
                available
        );
    }

    /** 4-12 AI 피드백 상세 조회 */
    public AiFeedbackDetailResponse getAiFeedbackDetail(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        requireActiveMember(project.getId(), memberId);
        if (!project.isEnded()) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_COMPLETE);
        }

        AIFeedback aiFeedback = aiFeedbackRepository.findByProjectIdAndMemberId(projectId, memberId)
                .orElseGet(() -> generateIfDeadlinePassed(project, memberId));

        return new AiFeedbackDetailResponse(
                project.getId(),
                List.of("#꼼꼼한", "#책임감"),
                aiFeedback.getStrength(),
                aiFeedback.getWeakness()
        );
    }

    /**
     * 배치: 평가 기간(3일)이 지난 종료 프로젝트 전체를 대상으로, 아직 AI 피드백이 생성되지 않은
     * 팀원 중 최소 평가 인원을 충족한 경우 일괄 생성한다. (day-plan: 매일 자정 실행)
     */
    @Transactional
    public void generatePendingAiFeedbackBatch() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(EVALUATION_PERIOD_DAYS);
        List<Project> targets = projectRepository.findAllByStatusAndEndedAtLessThanEqual(ProjectStatus.END, threshold);
        for (Project project : targets) {
            List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId());
            int required = requiredEvaluatorCount(members.size());
            for (ProjectMember pm : members) {
                Long revieweeId = pm.getMember().getId();
                if (aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), revieweeId).isPresent()) {
                    continue;
                }
                long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), revieweeId);
                if (evaluatedCount >= required) {
                    generateAiFeedbackMock(project, revieweeId);
                }
            }
        }
    }

    /** 평가 기간(3일)이 지났는데도 AI 피드백이 없는 경우, 조회 시점에 배치 생성을 대신 수행한다. */
    private AIFeedback generateIfDeadlinePassed(Project project, Long memberId) {
        LocalDateTime deadline = evaluationDeadline(project);
        if (LocalDateTime.now().isBefore(deadline)) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_COMPLETE);
        }
        int teamSize = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId()).size();
        long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), memberId);
        if (evaluatedCount < requiredEvaluatorCount(teamSize)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_EVALUATION);
        }
        generateAiFeedbackMock(project, memberId);
        return aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
    }

    /** 팀 인원(N)에 따른 AI 피드백 생성 최소 평가 인원. 2인 이하는 생성 대상 아님. */
    private int requiredEvaluatorCount(int teamSize) {
        if (teamSize < 3) {
            return 0;
        }
        return (int) Math.ceil(teamSize / 2.0);
    }

    /** 조기 완료 트리거: 본인 제외 팀원 전원이 평가를 완료하면 즉시 AI 피드백을 생성한다. */
    private void checkAndGenerateAiFeedback(Project project, Long revieweeId) {
        int teamSize = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId()).size();
        long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), revieweeId);
        if (teamSize >= 3 && evaluatedCount >= teamSize - 1) {
            generateAiFeedbackMock(project, revieweeId);
        }
    }

    private void generateAiFeedbackMock(Project project, Long revieweeId) {
        if (aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), revieweeId).isPresent()) {
            return;
        }
        Member reviewee = memberRepository.getReferenceById(revieweeId);
        aiFeedbackRepository.save(AIFeedback.builder()
                .project(project)
                .member(reviewee)
                .strength("(AI 생성 목업) 팀원들의 평가를 종합하면 책임감과 협업 능력이 뛰어납니다.")
                .weakness("(AI 생성 목업) 마감 기한 관리에 조금 더 신경쓰면 좋겠습니다.")
                .build());
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void requireActiveMember(Long projectId, Long memberId) {
        if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireEnded(Project project) {
        if (!project.isEnded()) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_ENDED);
        }
    }

    private void requireWithinDeadline(LocalDateTime deadline) {
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ErrorCode.EVALUATION_PERIOD_EXPIRED);
        }
    }

    private LocalDateTime evaluationDeadline(Project project) {
        return project.getEndedAt().plusDays(EVALUATION_PERIOD_DAYS);
    }
}

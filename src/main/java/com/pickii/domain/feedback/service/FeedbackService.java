package com.pickii.domain.feedback.service;

import com.pickii.domain.feedback.dto.AiFeedbackDetailResponse;
import com.pickii.domain.feedback.dto.EvaluationPeriodResponse;
import com.pickii.domain.feedback.dto.FeedbackCreateRequest;
import com.pickii.domain.feedback.dto.FeedbackListResponse;
import com.pickii.domain.feedback.dto.FeedbackTargetResponse;
import com.pickii.domain.feedback.dto.TargetMemberResponse;
import com.pickii.domain.feedback.dto.KeywordResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickii.domain.feedback.entity.AIFeedback;
import com.pickii.domain.feedback.entity.AIFeedbackKeyword;
import com.pickii.domain.feedback.entity.Feedback;
import com.pickii.domain.feedback.entity.Keyword;
import com.pickii.domain.feedback.repository.AIFeedbackKeywordRepository;
import com.pickii.domain.feedback.repository.AIFeedbackRepository;
import com.pickii.domain.feedback.repository.FeedbackRepository;
import com.pickii.domain.feedback.repository.KeywordRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.event.NotificationCreatedEvent;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.entity.ProjectStatus;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.global.ai.GeminiClient;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    /** AI 피드백 키워드는 최대 3개까지만 부여한다. */
    private static final int MAX_AI_FEEDBACK_KEYWORDS = 3;

    private final FeedbackRepository feedbackRepository;
    private final AIFeedbackRepository aiFeedbackRepository;
    private final AIFeedbackKeywordRepository aiFeedbackKeywordRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final KeywordRepository keywordRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 5-6 피드백 키워드 조회 (마스터 데이터, 거의 변경 없어 캐싱) */
    @Cacheable("feedbackKeywords")
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
        // requiredEvaluatorCount()는 2인 이하 팀에서 "생성 불가"를 표현하려고 MAX_VALUE를 반환하므로,
        // 화면 표시용 값은 그대로 노출하지 않고 0(대상 아님)으로 clamp한다.
        int requiredCount = teamSize < 3 ? 0 : requiredEvaluatorCount(teamSize);

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
                getAiFeedbackKeywords(aiFeedback.getId()),
                aiFeedback.getStrength(),
                aiFeedback.getWeakness()
        );
    }

    private List<String> getAiFeedbackKeywords(Long aiFeedbackId) {
        List<Long> keywordIds = aiFeedbackKeywordRepository.findAllByAiFeedbackId(aiFeedbackId).stream()
                .map(AIFeedbackKeyword::getKeywordId)
                .toList();
        return keywordRepository.findAllById(keywordIds).stream()
                .map(k -> "#" + k.getName())
                .toList();
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
                    generateAiFeedback(project, revieweeId);
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
        generateAiFeedback(project, memberId);
        return aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
    }

    /**
     * 팀 인원(N)에 따른 AI 피드백 생성 최소 평가 인원.
     * 2인 이하는 생성 대상 아님 — evaluatedCount(항상 0 이상)로는 절대 충족할 수 없도록
     * Integer.MAX_VALUE를 반환한다. (0을 반환하면 "평가 0건 >= 0건"이 항상 참이 되어
     * 2인 팀도 AI 피드백이 생성되던 버그가 있었다.)
     */
    private int requiredEvaluatorCount(int teamSize) {
        if (teamSize < 3) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(teamSize / 2.0);
    }

    /** 조기 완료 트리거: 본인 제외 팀원 전원이 평가를 완료하면 즉시 AI 피드백을 생성한다. */
    private void checkAndGenerateAiFeedback(Project project, Long revieweeId) {
        int teamSize = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId()).size();
        long evaluatedCount = feedbackRepository.countByProjectIdAndRevieweeId(project.getId(), revieweeId);
        if (teamSize >= 3 && evaluatedCount >= teamSize - 1) {
            generateAiFeedback(project, revieweeId);
        }
    }

    /** 4-11/4-12 AI 종합 피드백 생성 (Gemini). 팀원들의 상호평가를 종합해 강점/개선점 요약과 역량 키워드를 산출한다. */
    private void generateAiFeedback(Project project, Long revieweeId) {
        if (aiFeedbackRepository.findByProjectIdAndMemberId(project.getId(), revieweeId).isPresent()) {
            return;
        }
        Member reviewee = memberRepository.getReferenceById(revieweeId);
        List<Feedback> feedbacks = feedbackRepository.findAllByProjectIdAndRevieweeId(project.getId(), revieweeId);
        List<Keyword> keywordPool = keywordRepository.findAll();

        reviewee.gainExp(averageFeedbackScore(feedbacks));

        AiFeedbackResult result = requestAiFeedback(feedbacks, keywordPool);

        AIFeedback aiFeedback = aiFeedbackRepository.save(AIFeedback.builder()
                .project(project)
                .member(reviewee)
                .strength(result.strengthSummary())
                .weakness(result.weaknessSummary())
                .build());

        List<Long> poolIds = keywordPool.stream().map(Keyword::getId).toList();
        List<Long> chosenKeywordIds = result.keywordIds() == null
                ? List.of()
                : result.keywordIds().stream().distinct().filter(poolIds::contains).limit(MAX_AI_FEEDBACK_KEYWORDS).toList();
        chosenKeywordIds.forEach(keywordId ->
                aiFeedbackKeywordRepository.save(new AIFeedbackKeyword(keywordId, aiFeedback.getId())));

        notifyAiFeedbackReady(project, reviewee);
    }

    /** AI가 상호평가를 종합한 결과가 나왔음을 평가 대상 본인에게 알린다. */
    private void notifyAiFeedbackReady(Project project, Member reviewee) {
        NotificationSetting setting = notificationSettingRepository.findById(reviewee.getId()).orElse(null);
        if (setting == null || !setting.isProjectNoti()) {
            return;
        }
        String title = "AI 상호평가 리포트가 도착했습니다.";
        String content = "'" + project.getName() + "' 프로젝트의 AI 종합 리포트를 확인해보세요.";
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(reviewee)
                .title(title)
                .content(content)
                .type(NotificationType.FEEDBACK)
                .referenceType(NotificationReferenceType.FEEDBACK)
                .referenceId(project.getId())
                .build());
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                reviewee.getId(), title, content, NotificationType.FEEDBACK, NotificationReferenceType.FEEDBACK, project.getId()));
    }

    /**
     * 상호평가 5개 문항(각 1~5점) 합계를 리뷰어별로 구한 뒤, 리뷰어 수만큼 평균 내어
     * 경험치로 적립할 점수를 산출한다. (예: 5명 참여 시 본인 제외 4명이 준 점수의 평균)
     */
    private int averageFeedbackScore(List<Feedback> feedbacks) {
        int totalScore = feedbacks.stream()
                .mapToInt(f -> f.getCommitScore() + f.getCommScore() + f.getDeadlineScore()
                        + f.getCooperateScore() + f.getContributeScore())
                .sum();
        return Math.round((float) totalScore / feedbacks.size());
    }

    private AiFeedbackResult requestAiFeedback(List<Feedback> feedbacks, List<Keyword> keywordPool) {
        String reviewsText = IntStream.range(0, feedbacks.size())
                .mapToObj(i -> {
                    Feedback f = feedbacks.get(i);
                    return "평가자 " + (i + 1) + " - 책임감:" + f.getCommitScore()
                            + ", 소통:" + f.getCommScore()
                            + ", 기한준수:" + f.getDeadlineScore()
                            + ", 협업:" + f.getCooperateScore()
                            + ", 기여도:" + f.getContributeScore()
                            + " / 강점: " + f.getStrengthText()
                            + " / 개선점: " + f.getWeaknessText();
                })
                .collect(Collectors.joining("\n"));

        String keywordPoolText = keywordPool.isEmpty()
                ? "(없음)"
                : keywordPool.stream().map(k -> k.getId() + ":" + k.getName()).collect(Collectors.joining(", "));

        String prompt = """
                너는 팀 프로젝트 상호평가 결과를 종합 분석하는 도우미다.
                아래는 한 팀원이 다른 팀원들에게서 익명으로 받은 상호평가 결과다.
                이를 종합해 강점 요약(strengthSummary)과 개선점 요약(weaknessSummary)을 각각 300자 이내
                한국어로 작성하라. 없는 사실을 지어내지 말고, 평가자 개인을 특정하거나 인용하지 마라.

                또한 아래 [키워드 후보] 목록에서만 골라 이 팀원을 가장 잘 나타내는 키워드를
                **최대 3개까지만** 선택하라(keywordIds). 반드시 3개 이하여야 하며, 그중에서도
                가장 확실하게 드러나는 것만 우선순위를 매겨 골라라. 후보에 없는 키워드는 만들어내지
                말고, 후보가 비어 있으면 빈 배열을 반환하라.

                [상호평가 결과]
                %s

                [키워드 후보 (id:이름)]
                %s
                """.formatted(reviewsText, keywordPoolText);

        Map<String, Object> responseSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "strengthSummary", Map.of("type", "string"),
                        "weaknessSummary", Map.of("type", "string"),
                        "keywordIds", Map.of("type", "array", "items", Map.of("type", "integer"))
                ),
                "required", List.of("strengthSummary", "weaknessSummary", "keywordIds")
        );

        String json = geminiClient.generateJson(prompt, responseSchema);
        try {
            return objectMapper.readValue(json, AiFeedbackResult.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private record AiFeedbackResult(String strengthSummary, String weaknessSummary, List<Long> keywordIds) {
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

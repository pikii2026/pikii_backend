package com.pickii.domain.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickii.domain.feedback.dto.FeedbackCreateRequest;
import com.pickii.domain.feedback.dto.ScoresRequest;
import com.pickii.domain.feedback.entity.AIFeedback;
import com.pickii.domain.feedback.entity.Feedback;
import com.pickii.domain.feedback.repository.AIFeedbackKeywordRepository;
import com.pickii.domain.feedback.repository.AIFeedbackRepository;
import com.pickii.domain.feedback.repository.FeedbackRepository;
import com.pickii.domain.feedback.repository.KeywordRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.global.ai.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 상호평가(Feedback) 종합 시점에 리뷰이(revieweeId)에게 경험치(exp)가
 * 올바르게, 그리고 중복 없이 적립되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long REVIEWER_ID = 10L;
    private static final Long REVIEWEE_ID = 20L;

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private AIFeedbackRepository aiFeedbackRepository;
    @Mock private AIFeedbackKeywordRepository aiFeedbackKeywordRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private KeywordRepository keywordRepository;
    @Mock private GeminiClient geminiClient;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(
                feedbackRepository, aiFeedbackRepository, aiFeedbackKeywordRepository,
                projectRepository, projectMemberRepository, memberRepository, keywordRepository,
                geminiClient, new ObjectMapper());
    }

    @Test
    void write_전원평가완료시_받은_점수_평균만큼_경험치를_적립한다() {
        Member reviewee = mock(Member.class);
        givenCommonWriteStubs(reviewee);

        // teamSize=3 → 조기완료 트리거 조건(evaluatedCount >= teamSize-1 = 2) 충족
        given(projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(PROJECT_ID)).willReturn(teamOfSize(3));
        given(feedbackRepository.countByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID)).willReturn(2L);
        given(aiFeedbackRepository.findByProjectIdAndMemberId(PROJECT_ID, REVIEWEE_ID)).willReturn(Optional.empty());

        // 본인 제외 2명이 준 점수: 합계 25점, 합계 15점 → 평균 20점
        given(feedbackRepository.findAllByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID))
                .willReturn(List.of(feedbackWithScores(5, 5, 5, 5, 5), feedbackWithScores(3, 3, 3, 3, 3)));
        givenGeminiSucceeds();

        feedbackService.write(REVIEWER_ID, createRequest());

        verify(reviewee).gainExp(20);
    }

    @Test
    void write_평균점수가_정수가_아니면_반올림해서_경험치를_적립한다() {
        Member reviewee = mock(Member.class);
        givenCommonWriteStubs(reviewee);

        given(projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(PROJECT_ID)).willReturn(teamOfSize(3));
        given(feedbackRepository.countByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID)).willReturn(2L);
        given(aiFeedbackRepository.findByProjectIdAndMemberId(PROJECT_ID, REVIEWEE_ID)).willReturn(Optional.empty());

        // 합계 21점, 22점 → 평균 21.5점 → 반올림하여 22점 적립
        given(feedbackRepository.findAllByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID))
                .willReturn(List.of(feedbackWithScores(5, 4, 4, 4, 4), feedbackWithScores(5, 5, 4, 4, 4)));
        givenGeminiSucceeds();

        feedbackService.write(REVIEWER_ID, createRequest());

        verify(reviewee).gainExp(22);
    }

    @Test
    void write_이미_AI피드백이_생성된_경우_경험치를_중복_적립하지_않는다() {
        Member reviewee = mock(Member.class);
        givenCommonWriteStubs(reviewee);

        given(projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(PROJECT_ID)).willReturn(teamOfSize(3));
        given(feedbackRepository.countByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID)).willReturn(2L);
        // 이미 다른 트리거(배치 등)로 AI 피드백이 생성된 상태
        given(aiFeedbackRepository.findByProjectIdAndMemberId(PROJECT_ID, REVIEWEE_ID))
                .willReturn(Optional.of(mock(AIFeedback.class)));

        feedbackService.write(REVIEWER_ID, createRequest());

        verify(reviewee, never()).gainExp(any(Integer.class));
        verify(aiFeedbackRepository, never()).save(any());
        verify(geminiClient, never()).generateJson(anyString(), anyMap());
    }

    @Test
    void write_팀원_전원이_아직_평가를_마치지_않았다면_경험치를_적립하지_않는다() {
        Member reviewee = mock(Member.class);
        givenCommonWriteStubs(reviewee);

        // teamSize=4 → 조기완료에는 evaluatedCount >= 3 필요하지만 아직 1건뿐
        given(projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(PROJECT_ID)).willReturn(teamOfSize(4));
        given(feedbackRepository.countByProjectIdAndRevieweeId(PROJECT_ID, REVIEWEE_ID)).willReturn(1L);

        feedbackService.write(REVIEWER_ID, createRequest());

        verify(reviewee, never()).gainExp(any(Integer.class));
        verify(aiFeedbackRepository, never()).findByProjectIdAndMemberId(any(), any());
    }

    private void givenCommonWriteStubs(Member reviewee) {
        Project project = endedProject();
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(PROJECT_ID, REVIEWER_ID)).willReturn(true);
        given(projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(PROJECT_ID, REVIEWEE_ID)).willReturn(true);
        given(feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(PROJECT_ID, REVIEWER_ID, REVIEWEE_ID)).willReturn(false);
        given(memberRepository.getReferenceById(REVIEWER_ID)).willReturn(mock(Member.class));
        given(memberRepository.getReferenceById(REVIEWEE_ID)).willReturn(reviewee);
    }

    private void givenGeminiSucceeds() {
        given(keywordRepository.findAll()).willReturn(List.of());
        given(geminiClient.generateJson(anyString(), anyMap()))
                .willReturn("{\"strengthSummary\":\"성실함\",\"weaknessSummary\":\"소통 보완 필요\",\"keywordIds\":[]}");
        given(aiFeedbackRepository.save(any(AIFeedback.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private Project endedProject() {
        Project project = Project.builder()
                .recruit(null)
                .name("픽키 팀 프로젝트")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .build();
        project.end();
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    private FeedbackCreateRequest createRequest() {
        return new FeedbackCreateRequest(
                PROJECT_ID,
                REVIEWEE_ID,
                new ScoresRequest(5, 4, 5, 4, 5),
                "장점 텍스트는 최소 서른 글자 이상이어야 통과합니다 테스트용 문장입니다",
                "개선점 텍스트도 최소 서른 글자 이상이어야 통과합니다 테스트용 문장");
    }

    private List<ProjectMember> teamOfSize(int size) {
        List<ProjectMember> members = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            members.add(ProjectMember.builder().project(null).member(null).isLeader(false).build());
        }
        return members;
    }

    private Feedback feedbackWithScores(int commit, int comm, int deadline, int cooperate, int contribute) {
        return Feedback.builder()
                .project(null)
                .reviewer(null)
                .reviewee(null)
                .commitScore(commit)
                .commScore(comm)
                .deadlineScore(deadline)
                .cooperateScore(cooperate)
                .contributeScore(contribute)
                .strengthText("강점 텍스트")
                .weaknessText("개선점 텍스트")
                .build();
    }
}

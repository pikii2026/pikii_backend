package com.pickii.domain.feedback.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팀원 다면 상호평가 (5개 문항 1~5점 + 주관식 강점/개선점).
 * 프로젝트 종료(END) 후 3일간만 작성 가능하다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "reviewer_id", "reviewee_id"}))
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** 평가자 (탈퇴 시 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Member reviewer;

    /** 평가 대상자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id")
    private Member reviewee;

    /** [Q1] 맡은 역할 책임감 (1~5) */
    @Column(nullable = false)
    private int commitScore;

    /** [Q2] 연락과 진행 상황 공유 (1~5) */
    @Column(nullable = false)
    private int commScore;

    /** [Q3] 마감 기한 준수 (1~5) */
    @Column(nullable = false)
    private int deadlineScore;

    /** [Q4] 팀원 의견 존중 및 협업 (1~5) */
    @Column(nullable = false)
    private int cooperateScore;

    /** [Q5] 결과물 실질적 기여 (1~5) */
    @Column(nullable = false)
    private int contributeScore;

    /** 주관식: 강점 (30~500자) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengthText;

    /** 주관식: 개선점 (30~500자) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String weaknessText;

    @Builder
    public Feedback(Project project, Member reviewer, Member reviewee,
                    int commitScore, int commScore, int deadlineScore,
                    int cooperateScore, int contributeScore,
                    String strengthText, String weaknessText) {
        this.project = project;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.commitScore = commitScore;
        this.commScore = commScore;
        this.deadlineScore = deadlineScore;
        this.cooperateScore = cooperateScore;
        this.contributeScore = contributeScore;
        this.strengthText = strengthText;
        this.weaknessText = weaknessText;
    }
}

package com.pickii.domain.feedback.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI 종합 피드백 리포트.
 * 전원 평가 완료 시 즉시 생성, 또는 종료+3일 시점에 배치 생성.
 * 최소 평가 인원 ceil(N/2) 충족 필요 (2인 팀은 생성 제외).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AIFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** 피드백 대상 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    /** AI 종합 칭찬 */
    @Column(columnDefinition = "TEXT")
    private String strength;

    /** AI 종합 개선점 */
    @Column(columnDefinition = "TEXT")
    private String weakness;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AIFeedback(Project project, Member member, String strength, String weakness) {
        this.project = project;
        this.member = member;
        this.strength = strength;
        this.weakness = weakness;
    }
}

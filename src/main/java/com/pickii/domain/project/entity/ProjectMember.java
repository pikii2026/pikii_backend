package com.pickii.domain.project.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 참여자 명단. leftAt이 NULL이면 참여 중.
 * 상호평가 대상 인원(N)은 종료 시점 leftAt IS NULL 기준으로 확정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private boolean isLeader;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /** 중도 하차/퇴출 일시 (NULL이면 참여 중) */
    private LocalDateTime leftAt;

    @Builder
    public ProjectMember(Project project, Member member, boolean isLeader) {
        this.project = project;
        this.member = member;
        this.isLeader = isLeader;
        this.joinedAt = LocalDateTime.now();
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return leftAt == null;
    }

    /** 프로젝트장 위임 */
    public void delegateLeader() {
        this.isLeader = false;
    }

    public void promoteLeader() {
        this.isLeader = true;
    }
}

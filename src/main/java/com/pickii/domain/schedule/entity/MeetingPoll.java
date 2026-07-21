package com.pickii.domain.schedule.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 회의 일정 조율 (When2meet 방식).
 * 30분 단위 후보 슬롯을 생성하고 팀원이 가능/불가를 1회 제출, 프로젝트장이 최종 확정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MeetingPoll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** 조율을 시작한 프로젝트장 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @Column(nullable = false, length = 20)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingPollStatus status;

    /** 회의 소요 시간(분) - 30 / 60 / 90 / 120 */
    @Column(nullable = false)
    private int durationMin;

    @Column(nullable = false)
    private LocalDate rangeStart;

    @Column(nullable = false)
    private LocalDate rangeEnd;

    @Column(nullable = false)
    private LocalTime dayStart;

    @Column(nullable = false)
    private LocalTime dayEnd;

    /** 응답 마감 시각 (기본 생성 + 12시간, 프로젝트장 조정 가능) */
    @Column(nullable = false)
    private LocalDateTime deadline;

    /** 확정된 팀 일정 (확정 전 null) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private PartySchedule schedule;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MeetingPoll(Project project, Member createdBy, String title, int durationMin,
                       LocalDate rangeStart, LocalDate rangeEnd,
                       LocalTime dayStart, LocalTime dayEnd, LocalDateTime deadline) {
        this.project = project;
        this.createdBy = createdBy;
        this.title = title;
        this.status = MeetingPollStatus.COLLECTING;
        this.durationMin = durationMin;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
        this.deadline = deadline;
    }

    public void confirm(PartySchedule schedule) {
        this.status = MeetingPollStatus.CONFIRMED;
        this.schedule = schedule;
    }

    public void cancel() {
        this.status = MeetingPollStatus.CANCELLED;
        this.schedule = null;
    }

    public boolean isCollecting() {
        return this.status == MeetingPollStatus.COLLECTING;
    }
}

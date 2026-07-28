package com.pickii.domain.schedule.entity;

import com.pickii.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 팀 최종 확정 일정 (회의 조율 확정 또는 프로젝트장 직접 등록).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String rrule;

    @Column(columnDefinition = "TEXT")
    private String exDate;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public PartySchedule(Project project, LocalDate startDate, LocalDate endDate,
                         LocalTime startTime, LocalTime endTime, String rrule,
                         String exDate, String title, String content) {
        this.project = project;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
        this.exDate = exDate;
        this.title = title;
        this.content = content;
    }

    public boolean isRecurring() {
        return rrule != null;
    }

    /** 7-17 팀 일정 수정. 단발↔반복 전환은 지원하지 않고 호출부에서 기존 종류에 맞는 값만 전달한다. */
    public void update(String title, LocalDate startDate, LocalDate endDate,
                        LocalTime startTime, LocalTime endTime, String rrule, String content) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
        this.content = content;
    }
}

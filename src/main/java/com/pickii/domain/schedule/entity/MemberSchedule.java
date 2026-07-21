package com.pickii.domain.schedule.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 개인 일정.
 * 단발 일정: rrule = null, startDate == endDate.
 * 반복 일정: RFC 5545 RRULE 저장, 조회 범위 내에서 애플리케이션이 전개(ical4j).
 * 무한 반복 금지 (endDate 필수). 특정 회차 제외는 exDate로 처리.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** RFC 5545 반복 규칙 (예: FREQ=WEEKLY;BYDAY=MO,WE). 단발이면 null */
    private String rrule;

    /** 반복에서 제외할 날짜 목록 (콤마 구분, RFC 5545 EXDATE) */
    @Column(columnDefinition = "TEXT")
    private String exDate;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sc_id")
    private ScheduleCategory category;

    @Builder
    public MemberSchedule(Member member, LocalDate startDate, LocalDate endDate,
                          LocalTime startTime, LocalTime endTime, String rrule,
                          String exDate, String title, String content, ScheduleCategory category) {
        this.member = member;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
        this.exDate = exDate;
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public boolean isRecurring() {
        return rrule != null;
    }
}

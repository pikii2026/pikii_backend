package com.pickii.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회의 시간 후보 슬롯.
 * 탐색 기간 × 탐색 시간대를 30분 단위로 나누어 전체 그리드로 생성한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingPollSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id")
    private MeetingPoll poll;

    @Column(nullable = false)
    private LocalDateTime startAt;

    /** 슬롯 종료 일시 (소요 시간 반영) */
    @Column(nullable = false)
    private LocalDateTime endAt;

    @Builder
    public MeetingPollSlot(MeetingPoll poll, LocalDateTime startAt, LocalDateTime endAt) {
        this.poll = poll;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}

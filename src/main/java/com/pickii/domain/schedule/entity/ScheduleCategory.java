package com.pickii.domain.schedule.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 카테고리 (제목 + 색상). 사용자가 직접 만들어 일정 생성 시 선택한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String title;

    /** 색상 값 (예: #FF5733) */
    @Column(nullable = false, length = 20)
    private String color;

    @Builder
    public ScheduleCategory(Member member, String title, String color) {
        this.member = member;
        this.title = title;
        this.color = color;
    }

    public void update(String title, String color) {
        this.title = title;
        this.color = color;
    }
}

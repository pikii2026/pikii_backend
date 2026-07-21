package com.pickii.domain.resume.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 수상 및 경험 이력 (구조화) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetailExperience extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    /** 활동명 / 직무명 / 수상명 */
    @Column(nullable = false)
    private String title;

    /** 소속/주관 기관명 */
    private String organization;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    /** 진행 중이면 null */
    private LocalDate endDate;

    @Builder
    public DetailExperience(Member member, String title, String organization,
                            String description, LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.title = title;
        this.organization = organization;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}

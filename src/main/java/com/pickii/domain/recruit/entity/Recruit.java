package com.pickii.domain.recruit.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 구인/모집 공고. Soft Delete(deletedAt) 적용.
 * 작성자 탈퇴 시 member는 null이 되며 화면에서 '알 수 없음'으로 표시한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 20)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitStatus status;

    /** 교내(true) / 교외(false) 공고 구분 */
    @Column(nullable = false)
    private boolean onCampus;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** 한 줄 소개 (50자 이하) */
    @Column(length = 50)
    private String simpleDesc;

    @Lob
    @Column(nullable = false)
    private String content;

    /** 모집 인원 (1~7) */
    @Column(nullable = false)
    private int targetCount;

    /** 현재 참여 인원 (지원 수락 시 +1, 이탈/퇴출 시 -1) */
    @Column(nullable = false)
    private int currentCount;

    private LocalDateTime deletedAt;

    @Builder
    public Recruit(Member member, String title, boolean onCampus, LocalDate startDate,
                   LocalDate endDate, String simpleDesc, String content, int targetCount) {
        this.member = member;
        this.title = title;
        this.status = RecruitStatus.OPEN;
        this.onCampus = onCampus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.simpleDesc = simpleDesc;
        this.content = content;
        this.targetCount = targetCount;
        this.currentCount = 0;
    }

    public int getAvailableSlots() {
        return targetCount - currentCount;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(String title, boolean onCampus, LocalDate startDate, LocalDate endDate,
                        String simpleDesc, String content, int targetCount) {
        this.title = title;
        this.onCampus = onCampus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.simpleDesc = simpleDesc;
        this.content = content;
        this.targetCount = targetCount;
    }

    public void close() {
        this.status = RecruitStatus.CLOSED;
    }

    public void openAdditional() {
        this.status = RecruitStatus.ADDITIONAL;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseCurrentCount() {
        this.currentCount++;
    }

    public void decreaseCurrentCount() {
        this.currentCount = Math.max(0, this.currentCount - 1);
    }
}

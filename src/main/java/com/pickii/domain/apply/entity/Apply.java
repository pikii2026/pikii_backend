package com.pickii.domain.apply.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서. 동일 공고 중복 지원 불가 (Unique(recruit, member)).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"recruit_id", "member_id"}))
public class Apply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruit_id")
    private Recruit recruit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    /** 지원 메시지 (300자 이하) */
    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplyStatus status;

    @Builder
    public Apply(Recruit recruit, Member member, String message) {
        this.recruit = recruit;
        this.member = member;
        this.message = message;
        this.status = ApplyStatus.WAITING;
    }

    public void accept() {
        this.status = ApplyStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ApplyStatus.REJECTED;
    }

    public boolean isWaiting() {
        return this.status == ApplyStatus.WAITING;
    }
}

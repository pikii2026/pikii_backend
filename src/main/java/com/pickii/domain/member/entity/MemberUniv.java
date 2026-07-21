package com.pickii.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 학교 정보 (Member와 1:1).
 * 학교는 마스터에서 선택(FK), 전공은 사용자 직접 입력.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberUniv {

    /** Member PK를 그대로 PK로 사용 (1:1 매핑) */
    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "univ_id")
    private Univ univ;

    @Column(nullable = false, length = 50)
    private String major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcademicStatus status;

    @Builder
    public MemberUniv(Member member, Univ univ, String major, AcademicStatus status) {
        this.member = member;
        this.univ = univ;
        this.major = major;
        this.status = status;
    }
}

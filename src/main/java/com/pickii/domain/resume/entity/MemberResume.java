package com.pickii.domain.resume.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 = 이력서 (Member와 1:1).
 * contactEmail은 이력서 생성 시점에 가입 이메일을 복사해 초기값으로 넣는다.
 * aboutMe는 최초 1회 AI가 생성하고 이후 사용자가 직접 수정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberResume extends BaseTimeEntity {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String contactEmail;

    /** 희망 진로 (100자 이하) */
    @Column(length = 100)
    private String hope;

    /** 본인의 장점 (300자 이하, 사용자 직접 입력) */
    @Column(columnDefinition = "TEXT")
    private String strength;

    /** 자기소개 (최초 AI 생성) */
    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    @Builder
    public MemberResume(Member member, String contactEmail, String hope, String strength, String aboutMe) {
        this.member = member;
        this.contactEmail = contactEmail;
        this.hope = hope;
        this.strength = strength;
        this.aboutMe = aboutMe;
    }
}

package com.pickii.domain.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 사용자 보유 기술 스택 매핑. level은 1(하)~3(상). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MemberTechStack.Pk.class)
public class MemberTechStack {

    @Id
    private Long memberId;

    @Id
    private Long techStackId;

    @Column(nullable = false)
    private int level;

    public MemberTechStack(Long memberId, Long techStackId, int level) {
        this.memberId = memberId;
        this.techStackId = techStackId;
        this.level = level;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long memberId;
        private Long techStackId;
    }
}

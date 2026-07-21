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
import java.time.LocalDate;

/** 사용자 보유 자격증 (취득일 포함) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MemberLicense.Pk.class)
public class MemberLicense {

    @Id
    private Long memberId;

    @Id
    private Long licenseId;

    @Column(nullable = false)
    private LocalDate acquired;

    public MemberLicense(Long memberId, Long licenseId, LocalDate acquired) {
        this.memberId = memberId;
        this.licenseId = licenseId;
        this.acquired = acquired;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long memberId;
        private Long licenseId;
    }
}

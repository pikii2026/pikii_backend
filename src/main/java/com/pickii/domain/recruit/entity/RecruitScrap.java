package com.pickii.domain.recruit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 공고 스크랩(북마크). 동일 공고 중복 스크랩 불가(복합 PK). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@IdClass(RecruitScrap.Pk.class)
public class RecruitScrap {

    @Id
    private Long memberId;

    @Id
    private Long recruitId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RecruitScrap(Long memberId, Long recruitId) {
        this.memberId = memberId;
        this.recruitId = recruitId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long memberId;
        private Long recruitId;
    }
}

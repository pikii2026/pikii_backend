package com.pickii.domain.recruit.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 공고 ↔ 카테고리 매핑 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(RecruitCategory.Pk.class)
public class RecruitCategory {

    @Id
    private Long recruitId;

    @Id
    private Long categoryId;

    public RecruitCategory(Long recruitId, Long categoryId) {
        this.recruitId = recruitId;
        this.categoryId = categoryId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long recruitId;
        private Long categoryId;
    }
}

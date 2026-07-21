package com.pickii.domain.apply.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 지원서 ↔ 선택 키워드 매핑 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ApplyKeywordMap.Pk.class)
public class ApplyKeywordMap {

    @Id
    private Long applyId;

    @Id
    private Long keywordId;

    public ApplyKeywordMap(Long applyId, Long keywordId) {
        this.applyId = applyId;
        this.keywordId = keywordId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long applyId;
        private Long keywordId;
    }
}

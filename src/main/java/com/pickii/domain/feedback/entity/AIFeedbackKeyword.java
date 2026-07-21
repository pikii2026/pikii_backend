package com.pickii.domain.feedback.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** AI 피드백 ↔ 획득 키워드 매핑 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(AIFeedbackKeyword.Pk.class)
public class AIFeedbackKeyword {

    @Id
    private Long keywordId;

    @Id
    private Long aiFeedbackId;

    public AIFeedbackKeyword(Long keywordId, Long aiFeedbackId) {
        this.keywordId = keywordId;
        this.aiFeedbackId = aiFeedbackId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long keywordId;
        private Long aiFeedbackId;
    }
}

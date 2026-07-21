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

/** 공고 ↔ 주제 매핑 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(RecruitTopic.Pk.class)
public class RecruitTopic {

    @Id
    private Long recruitId;

    @Id
    private Long topicId;

    public RecruitTopic(Long recruitId, Long topicId) {
        this.recruitId = recruitId;
        this.topicId = topicId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long recruitId;
        private Long topicId;
    }
}

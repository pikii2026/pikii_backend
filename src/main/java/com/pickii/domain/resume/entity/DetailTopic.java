package com.pickii.domain.resume.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 사용자 관심 주제 매핑 (Topic 마스터 참조) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(DetailTopic.Pk.class)
public class DetailTopic {

    @Id
    private Long memberId;

    @Id
    private Long topicId;

    public DetailTopic(Long memberId, Long topicId) {
        this.memberId = memberId;
        this.topicId = topicId;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long memberId;
        private Long topicId;
    }
}

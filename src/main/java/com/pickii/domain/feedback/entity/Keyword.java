package com.pickii.domain.feedback.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드백 분석용 역량 키워드 풀 (Master Table) - 예: 원활한 소통
 * 지원 어필용 ApplyKeyword와는 용도가 다르다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public Keyword(String name) {
        this.name = name;
    }
}

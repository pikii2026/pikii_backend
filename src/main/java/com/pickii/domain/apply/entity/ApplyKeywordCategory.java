package com.pickii.domain.apply.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원 키워드 카테고리 (Master Table) - 예: 실행력/책임감, 기획/아이디어
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyKeywordCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public ApplyKeywordCategory(String name) {
        this.name = name;
    }
}

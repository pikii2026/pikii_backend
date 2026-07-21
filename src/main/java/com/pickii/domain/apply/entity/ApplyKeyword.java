package com.pickii.domain.apply.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원 키워드 (Master Table) - 예: "마감기한 잘 지켜요"
 * 지원 시 전체 카테고리 통틀어 최대 5개 선택 가능.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private ApplyKeywordCategory category;

    @Column(nullable = false)
    private String content;

    public ApplyKeyword(ApplyKeywordCategory category, String content) {
        this.category = category;
        this.content = content;
    }
}

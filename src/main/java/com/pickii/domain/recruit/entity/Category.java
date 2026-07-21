package com.pickii.domain.recruit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공고 카테고리 (Master Table) - 예: 공모전, 프로젝트, 스터디
 * 교내/교외 구분은 Category가 아니라 Recruit.onCampus로 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public Category(String name) {
        this.name = name;
    }
}

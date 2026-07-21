package com.pickii.domain.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기술 스택 기준 데이터 (Master Table) - 예: React, Spring, Figma
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechStack {

    public enum Type { SKILL, TOOL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** SKILL(프레임워크/언어) / TOOL(디자인·협업툴) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    public TechStack(String name, Type type) {
        this.name = name;
        this.type = type;
    }
}

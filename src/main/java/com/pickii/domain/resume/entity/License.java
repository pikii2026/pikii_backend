package com.pickii.domain.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자격증 목록 (Master Table) - 예: SQLD, ADsP, 컴활
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public License(String name) {
        this.name = name;
    }
}

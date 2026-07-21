package com.pickii.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대학교 기준 데이터 (Master Table).
 * 캠퍼스는 구분하지 않는다. 전국 대학 목록을 초기 시딩하며 사용자는 선택만 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Univ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public Univ(String name) {
        this.name = name;
    }
}

package com.pickii.domain.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 링크 카테고리 (Master Table) - 예: Git, Notion
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 플랫폼 브랜드 로고 이미지 URL */
    private String picUrl;

    public LinkCategory(String name, String picUrl) {
        this.name = name;
        this.picUrl = picUrl;
    }
}

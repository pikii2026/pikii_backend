package com.pickii.domain.resume.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 외부 포트폴리오 링크 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdditionalLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_cat_id")
    private LinkCategory linkCategory;

    @Column(nullable = false)
    private String url;

    @Builder
    public AdditionalLink(Member member, LinkCategory linkCategory, String url) {
        this.member = member;
        this.linkCategory = linkCategory;
        this.url = url;
    }
}

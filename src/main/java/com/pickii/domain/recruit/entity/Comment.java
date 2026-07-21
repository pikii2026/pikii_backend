package com.pickii.domain.recruit.entity;

import com.pickii.domain.member.entity.Member;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공고 질의응답 댓글. 대댓글은 Self Join(parent), 깊이 제한 없음.
 * Soft Delete 시 내용만 "삭제된 댓글입니다."로 표시하고 답글 구조는 유지한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruit_id")
    private Recruit recruit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @Column(nullable = false, length = 100)
    private String content;

    private LocalDateTime deletedAt;

    @Builder
    public Comment(Recruit recruit, Member member, Comment parent, String content) {
        this.recruit = recruit;
        this.member = member;
        this.parent = parent;
        this.content = content;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

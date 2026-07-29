package com.pickii.domain.notification.entity;

import com.pickii.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자별 알림 수신 설정 7종 (회원가입 시 함께 생성).
 * marketingNoti 초기값은 가입 시 pushNotiAgreed 동의 값을 사용한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationSetting {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private boolean chatNoti;

    @Column(nullable = false)
    private boolean applicantNoti;

    @Column(nullable = false)
    private boolean commentNoti;

    @Column(nullable = false)
    private boolean scheduleNoti;

    @Column(nullable = false)
    private boolean matchNoti;

    @Column(nullable = false)
    private boolean projectNoti;

    @Column(nullable = false)
    private boolean marketingNoti;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public NotificationSetting(Member member, boolean marketingNoti) {
        this.member = member;
        this.chatNoti = true;
        this.applicantNoti = true;
        this.commentNoti = true;
        this.scheduleNoti = true;
        this.matchNoti = true;
        this.projectNoti = true;
        this.marketingNoti = marketingNoti;
    }

    /** 9-6 알림 설정 수정 */
    public void update(boolean chatNoti, boolean applicantNoti, boolean commentNoti, boolean scheduleNoti,
                        boolean matchNoti, boolean projectNoti, boolean marketingNoti) {
        this.chatNoti = chatNoti;
        this.applicantNoti = applicantNoti;
        this.commentNoti = commentNoti;
        this.scheduleNoti = scheduleNoti;
        this.matchNoti = matchNoti;
        this.projectNoti = projectNoti;
        this.marketingNoti = marketingNoti;
    }
}

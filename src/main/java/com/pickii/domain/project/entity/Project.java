package com.pickii.domain.project.entity;

import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 본체.
 * 공고 작성자가 그룹 채팅을 생성하는 시점에 Recruit로부터 전환 생성된다.
 * (Project + ChatRoom(GROUP) + ProjectMember 동시 생성, ACCEPTED 지원자 1명 이상 필요)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruit_id")
    private Recruit recruit;

    /** 팀명 (그룹 채팅방 생성 시 입력한 채팅방 이름) */
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    /** 활동 종료일 (연장 시 갱신) */
    private LocalDate endDate;

    /** 종료 확인 알림 발송 시각 (3일 무응답 시 자동 종료 기산점) */
    private LocalDateTime endCheckedAt;

    @Builder
    public Project(Recruit recruit, String name, LocalDate startDate, LocalDate endDate) {
        this.recruit = recruit;
        this.name = name;
        this.status = ProjectStatus.IN_PROGRESS;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void end() {
        this.status = ProjectStatus.END;
    }

    /** 연장: EndDate 갱신 + 종료 확인 기산점 초기화 */
    public void extend(LocalDate newEndDate) {
        this.endDate = newEndDate;
        this.endCheckedAt = null;
    }

    public void markEndChecked() {
        this.endCheckedAt = LocalDateTime.now();
    }

    public boolean isEnded() {
        return this.status == ProjectStatus.END;
    }
}

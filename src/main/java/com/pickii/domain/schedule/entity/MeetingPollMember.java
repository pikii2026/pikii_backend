package com.pickii.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 조율 참여자별 응답 여부.
 * 조율 개설 시 참가 명단을 선택할 수 있다 (팀원 전원이 아니어도 됨).
 * 미응답자는 가능/불가 어느 쪽으로도 집계하지 않고 미응답으로 표시한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MeetingPollMember.Pk.class)
public class MeetingPollMember {

    @Id
    private Long pollId;

    @Id
    private Long memberId;

    @Column(nullable = false)
    private boolean responded;

    private LocalDateTime respondedAt;

    public MeetingPollMember(Long pollId, Long memberId) {
        this.pollId = pollId;
        this.memberId = memberId;
        this.responded = false;
    }

    public void markResponded() {
        this.responded = true;
        this.respondedAt = LocalDateTime.now();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long pollId;
        private Long memberId;
    }
}

package com.pickii.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 팀 일정(회의) 참석/불참 표시. 기본값은 참석(true).
 * 변경 시 그룹 채팅방에 시스템 메시지를 발송한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@IdClass(PartyScheduleAttendance.Pk.class)
public class PartyScheduleAttendance {

    @Id
    private Long scheduleId;

    @Id
    private Long memberId;

    @Column(nullable = false)
    private boolean attending;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PartyScheduleAttendance(Long scheduleId, Long memberId) {
        this.scheduleId = scheduleId;
        this.memberId = memberId;
        this.attending = true;
    }

    public void changeAttending(boolean attending) {
        this.attending = attending;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long scheduleId;
        private Long memberId;
    }
}

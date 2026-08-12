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

/**
 * 슬롯별 가능/불가 응답.
 * 개인 캘린더에 일정이 있는 슬롯은 '불가'로 프리필되어 제공된다. (캘린더는 초기값일 뿐)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MeetingPollAvailability.Pk.class)
public class MeetingPollAvailability {

    @Id
    private Long slotId;

    @Id
    private Long memberId;

    @Column(nullable = false)
    private boolean available;

    /** 조율 개설 시 개인 캘린더 겹침으로 시스템이 자동 생성한 응답인지 (팀원이 직접 제출하면 false로 바뀐다) */
    @Column(nullable = false)
    private boolean autoFilled;

    public MeetingPollAvailability(Long slotId, Long memberId, boolean available) {
        this.slotId = slotId;
        this.memberId = memberId;
        this.available = available;
        this.autoFilled = false;
    }

    /** 7-10 조율 개설 시 개인 캘린더와 겹치는 슬롯을 '불가'로 자동 생성한다. */
    public static MeetingPollAvailability autoFilledUnavailable(Long slotId, Long memberId) {
        MeetingPollAvailability availability = new MeetingPollAvailability(slotId, memberId, false);
        availability.autoFilled = true;
        return availability;
    }

    /** 7-12 응답 제출/재제출 시 기존 응답 갱신 (직접 제출한 값이므로 자동 생성 표시는 해제한다) */
    public void changeAvailable(boolean available) {
        this.available = available;
        this.autoFilled = false;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long slotId;
        private Long memberId;
    }
}

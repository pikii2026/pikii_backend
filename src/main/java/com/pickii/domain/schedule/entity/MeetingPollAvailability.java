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

    public MeetingPollAvailability(Long slotId, Long memberId, boolean available) {
        this.slotId = slotId;
        this.memberId = memberId;
        this.available = available;
    }

    /** 7-12 응답 재제출 시 기존 응답 갱신 */
    public void changeAvailable(boolean available) {
        this.available = available;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long slotId;
        private Long memberId;
    }
}

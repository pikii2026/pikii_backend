package com.pickii.domain.schedule.entity;

/**
 * 회의 일정 조율 상태 (ENUM.md 14)
 * 프로젝트당 COLLECTING 상태의 조율은 1개만 존재할 수 있다.
 */
public enum MeetingPollStatus {
    COLLECTING, CONFIRMED, CANCELLED
}

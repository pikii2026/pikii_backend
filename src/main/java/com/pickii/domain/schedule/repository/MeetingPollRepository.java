package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPoll;
import com.pickii.domain.schedule.entity.MeetingPollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingPollRepository extends JpaRepository<MeetingPoll, Long> {

    /** 프로젝트당 COLLECTING 조율은 1개만 존재 */
    Optional<MeetingPoll> findByProjectIdAndStatus(Long projectId, MeetingPollStatus status);

    boolean existsByProjectIdAndStatus(Long projectId, MeetingPollStatus status);

    /** 7-18 팀 일정 삭제 시, 조율로 확정된 일정이면 연결을 해제하기 위해 조회 */
    Optional<MeetingPoll> findByScheduleId(Long scheduleId);
}

package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPoll;
import com.pickii.domain.schedule.entity.MeetingPollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingPollRepository extends JpaRepository<MeetingPoll, Long> {

    /** 프로젝트당 COLLECTING 조율은 1개만 존재 */
    Optional<MeetingPoll> findByProjectIdAndStatus(Long projectId, MeetingPollStatus status);

    boolean existsByProjectIdAndStatus(Long projectId, MeetingPollStatus status);
}

package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingPollAvailabilityRepository extends JpaRepository<MeetingPollAvailability, MeetingPollAvailability.Pk> {

    List<MeetingPollAvailability> findAllBySlotIdIn(List<Long> slotIds);

    long countBySlotId(Long slotId);

    void deleteAllByMemberId(Long memberId);
}

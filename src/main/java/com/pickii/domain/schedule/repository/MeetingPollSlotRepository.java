package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingPollSlotRepository extends JpaRepository<MeetingPollSlot, Long> {

    List<MeetingPollSlot> findAllByPollIdOrderByStartAt(Long pollId);
}

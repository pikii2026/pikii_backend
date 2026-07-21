package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.PartySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyScheduleRepository extends JpaRepository<PartySchedule, Long> {

    List<PartySchedule> findAllByProjectId(Long projectId);
}

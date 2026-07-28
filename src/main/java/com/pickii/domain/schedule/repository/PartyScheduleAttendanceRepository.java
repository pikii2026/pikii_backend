package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.PartyScheduleAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyScheduleAttendanceRepository extends JpaRepository<PartyScheduleAttendance, PartyScheduleAttendance.Pk> {

    void deleteByScheduleId(Long scheduleId);
}

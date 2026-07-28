package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.PartySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PartyScheduleRepository extends JpaRepository<PartySchedule, Long> {

    List<PartySchedule> findAllByProjectId(Long projectId);

    /** 7-15 팀 일정 조회: 조회 범위와 겹치는 일정 (반복 일정은 애플리케이션에서 RRULE 전개) */
    List<PartySchedule> findAllByProjectIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long projectId, LocalDate rangeEnd, LocalDate rangeStart);
}

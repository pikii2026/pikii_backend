package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MemberSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MemberScheduleRepository extends JpaRepository<MemberSchedule, Long> {

    /** 조회 범위와 겹치는 일정 (반복 일정은 애플리케이션에서 RRULE 전개) */
    List<MemberSchedule> findAllByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, LocalDate rangeEnd, LocalDate rangeStart);
}

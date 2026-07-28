package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MemberSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MemberScheduleRepository extends JpaRepository<MemberSchedule, Long> {

    /** 조회 범위와 겹치는 일정 (반복 일정은 애플리케이션에서 RRULE 전개) */
    List<MemberSchedule> findAllByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, LocalDate rangeEnd, LocalDate rangeStart);

    /** 7-4 카테고리 삭제 시, 해당 카테고리를 쓰던 일정은 유지하되 category만 NULL 처리 */
    @Modifying
    @Query("UPDATE MemberSchedule m SET m.category = NULL WHERE m.category.id = :categoryId")
    void clearCategory(@Param("categoryId") Long categoryId);
}

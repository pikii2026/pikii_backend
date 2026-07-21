package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.ScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleCategoryRepository extends JpaRepository<ScheduleCategory, Long> {

    List<ScheduleCategory> findAllByMemberId(Long memberId);
}

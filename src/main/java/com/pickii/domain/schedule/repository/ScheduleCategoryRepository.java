package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.ScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleCategoryRepository extends JpaRepository<ScheduleCategory, Long> {

    List<ScheduleCategory> findAllByMemberId(Long memberId);

    /** 7-6/7-7 일정 생성 시 categoryId가 본인 카테고리인지 확인 */
    Optional<ScheduleCategory> findByIdAndMemberId(Long id, Long memberId);

    void deleteAllByMemberId(Long memberId);
}

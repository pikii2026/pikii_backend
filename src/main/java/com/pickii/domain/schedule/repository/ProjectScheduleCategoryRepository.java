package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.ProjectScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectScheduleCategoryRepository extends JpaRepository<ProjectScheduleCategory, ProjectScheduleCategory.Pk> {

    Optional<ProjectScheduleCategory> findByProjectIdAndScIdIn(Long projectId, List<Long> scIds);

    /** 7-4 카테고리 삭제 시, 해당 카테고리로 지정해둔 프로젝트 색상 매핑도 함께 정리한다 (고아 레코드 방지). */
    void deleteAllByScId(Long scId);
}

package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.ProjectScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectScheduleCategoryRepository extends JpaRepository<ProjectScheduleCategory, ProjectScheduleCategory.Pk> {

    Optional<ProjectScheduleCategory> findByProjectIdAndScIdIn(Long projectId, List<Long> scIds);
}

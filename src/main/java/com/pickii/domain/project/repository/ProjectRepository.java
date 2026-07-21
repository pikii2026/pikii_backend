package com.pickii.domain.project.repository;

import com.pickii.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByRecruitId(Long recruitId);

    boolean existsByRecruitId(Long recruitId);
}

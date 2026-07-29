package com.pickii.domain.project.repository;

import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByRecruitId(Long recruitId);

    boolean existsByRecruitId(Long recruitId);

    /** 배치: 진행기간이 끝났는데 아직 종료 확인 알림을 보내지 않은 프로젝트 */
    List<Project> findAllByStatusAndEndDateLessThanEqualAndEndCheckedAtIsNull(ProjectStatus status, LocalDate endDate);

    /** 배치: 종료 확인 알림 발송 후 임계시각(3일 전)까지도 무응답인 프로젝트 */
    List<Project> findAllByStatusAndEndCheckedAtIsNotNullAndEndCheckedAtLessThanEqual(
            ProjectStatus status, LocalDateTime endCheckedAtThreshold);

    /** 배치: 상호평가 기간(3일)이 지난 종료 프로젝트 */
    List<Project> findAllByStatusAndEndedAtLessThanEqual(ProjectStatus status, LocalDateTime endedAtThreshold);
}

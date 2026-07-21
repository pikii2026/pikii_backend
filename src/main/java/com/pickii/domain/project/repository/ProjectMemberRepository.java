package com.pickii.domain.project.repository;

import com.pickii.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** 참여 중인 팀원 목록 (leftAt IS NULL) */
    List<ProjectMember> findAllByProjectIdAndLeftAtIsNull(Long projectId);

    Optional<ProjectMember> findByProjectIdAndMemberIdAndLeftAtIsNull(Long projectId, Long memberId);

    boolean existsByProjectIdAndMemberIdAndLeftAtIsNull(Long projectId, Long memberId);
}

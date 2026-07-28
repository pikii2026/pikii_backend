package com.pickii.domain.project.repository;

import com.pickii.domain.project.entity.ProjectMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** 참여 중인 팀원 목록 (leftAt IS NULL) */
    List<ProjectMember> findAllByProjectIdAndLeftAtIsNull(Long projectId);

    Optional<ProjectMember> findByProjectIdAndMemberIdAndLeftAtIsNull(Long projectId, Long memberId);

    boolean existsByProjectIdAndMemberIdAndLeftAtIsNull(Long projectId, Long memberId);

    /** 4-11 내가 참여했던 종료(END)된 프로젝트 목록 */
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project p "
            + "WHERE pm.member.id = :memberId AND pm.leftAt IS NULL AND p.status = 'END'")
    Page<ProjectMember> findEndedProjectsByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}

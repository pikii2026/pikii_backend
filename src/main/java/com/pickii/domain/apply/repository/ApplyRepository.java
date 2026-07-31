package com.pickii.domain.apply.repository;

import com.pickii.domain.apply.entity.Apply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplyRepository extends JpaRepository<Apply, Long> {

    boolean existsByRecruitIdAndMemberId(Long recruitId, Long memberId);

    List<Apply> findAllByRecruitId(Long recruitId);

    List<Apply> findAllByMemberId(Long memberId);

    Page<Apply> findByMemberId(Long memberId, Pageable pageable);

    Page<Apply> findByRecruitId(Long recruitId, Pageable pageable);

    /** 회원 탈퇴 시 지원 내역은 유지하고 지원자만 '알 수 없음'으로 남긴다 (DB_Schema: MemberId ON DELETE SET NULL) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Apply a SET a.member = NULL WHERE a.member.id = :memberId")
    void detachMember(@Param("memberId") Long memberId);
}

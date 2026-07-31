package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Recruit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 2-1 검색/필터(키워드, 카테고리, 주제, onCampus) 동적 쿼리는 {@link RecruitSpecification} 참고 */
public interface RecruitRepository extends JpaRepository<Recruit, Long>, JpaSpecificationExecutor<Recruit> {

    Page<Recruit> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    /** 회원 탈퇴 시 작성 공고는 유지하고 작성자만 '알 수 없음'으로 남긴다 (DB_Schema: MemberId ON DELETE SET NULL) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Recruit r SET r.member = NULL WHERE r.member.id = :memberId")
    void detachMember(@Param("memberId") Long memberId);
}

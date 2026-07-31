package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.RecruitScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitScrapRepository extends JpaRepository<RecruitScrap, RecruitScrap.Pk> {

    boolean existsByMemberIdAndRecruitId(Long memberId, Long recruitId);

    void deleteByMemberIdAndRecruitId(Long memberId, Long recruitId);

    void deleteAllByMemberId(Long memberId);

    @Query(value = "select rs from RecruitScrap rs join Recruit r on r.id = rs.recruitId "
            + "where rs.memberId = :memberId and r.deletedAt is null",
            countQuery = "select count(rs) from RecruitScrap rs join Recruit r on r.id = rs.recruitId "
                    + "where rs.memberId = :memberId and r.deletedAt is null")
    Page<RecruitScrap> findAllByMemberIdWithActiveRecruit(@Param("memberId") Long memberId, Pageable pageable);
}

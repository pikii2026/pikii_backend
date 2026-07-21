package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.RecruitScrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitScrapRepository extends JpaRepository<RecruitScrap, RecruitScrap.Pk> {

    boolean existsByMemberIdAndRecruitId(Long memberId, Long recruitId);

    void deleteByMemberIdAndRecruitId(Long memberId, Long recruitId);
}

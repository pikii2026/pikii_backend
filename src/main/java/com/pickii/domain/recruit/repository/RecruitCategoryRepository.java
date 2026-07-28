package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.RecruitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitCategoryRepository extends JpaRepository<RecruitCategory, RecruitCategory.Pk> {

    @Query("select rc.categoryId from RecruitCategory rc where rc.recruitId = :recruitId")
    List<Long> findCategoryIdsByRecruitId(@Param("recruitId") Long recruitId);

    void deleteAllByRecruitId(Long recruitId);
}

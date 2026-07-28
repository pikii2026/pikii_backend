package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.RecruitTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitTopicRepository extends JpaRepository<RecruitTopic, RecruitTopic.Pk> {

    @Query("select rt.topicId from RecruitTopic rt where rt.recruitId = :recruitId")
    List<Long> findTopicIdsByRecruitId(@Param("recruitId") Long recruitId);
}

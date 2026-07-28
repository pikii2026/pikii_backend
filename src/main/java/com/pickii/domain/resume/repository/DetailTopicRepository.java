package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.DetailTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetailTopicRepository extends JpaRepository<DetailTopic, DetailTopic.Pk> {

    @Query("select dt.topicId from DetailTopic dt where dt.memberId = :memberId")
    List<Long> findTopicIdsByMemberId(@Param("memberId") Long memberId);

    void deleteAllByMemberId(Long memberId);
}

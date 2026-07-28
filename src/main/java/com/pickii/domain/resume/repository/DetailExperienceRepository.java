package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.DetailExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetailExperienceRepository extends JpaRepository<DetailExperience, Long> {

    List<DetailExperience> findAllByMemberId(Long memberId);

    @Modifying
    @Query("delete from DetailExperience e where e.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}

package com.pickii.domain.apply.repository;

import com.pickii.domain.apply.entity.Apply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplyRepository extends JpaRepository<Apply, Long> {

    boolean existsByRecruitIdAndMemberId(Long recruitId, Long memberId);

    List<Apply> findAllByRecruitId(Long recruitId);

    List<Apply> findAllByMemberId(Long memberId);

    Page<Apply> findByMemberId(Long memberId, Pageable pageable);

    Page<Apply> findByRecruitId(Long recruitId, Pageable pageable);
}

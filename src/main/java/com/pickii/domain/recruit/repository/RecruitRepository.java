package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Recruit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitRepository extends JpaRepository<Recruit, Long> {
    // TODO: 검색/필터(키워드, 카테고리, 주제, onCampus) 동적 쿼리는 Specification 또는 QueryDSL로 구현
}

package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Recruit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 2-1 검색/필터(키워드, 카테고리, 주제, onCampus) 동적 쿼리는 {@link RecruitSpecification} 참고 */
public interface RecruitRepository extends JpaRepository<Recruit, Long>, JpaSpecificationExecutor<Recruit> {

    Page<Recruit> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);
}

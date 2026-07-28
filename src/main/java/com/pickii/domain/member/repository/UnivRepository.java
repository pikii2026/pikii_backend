package com.pickii.domain.member.repository;

import com.pickii.domain.member.entity.Univ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnivRepository extends JpaRepository<Univ, Long> {

    /** 5-8 대학교명 부분 검색 */
    List<Univ> findByNameContaining(String keyword);
}

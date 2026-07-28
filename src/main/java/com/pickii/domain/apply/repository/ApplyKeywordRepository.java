package com.pickii.domain.apply.repository;

import com.pickii.domain.apply.entity.ApplyKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ApplyKeywordRepository extends JpaRepository<ApplyKeyword, Long> {

    long countByIdIn(Collection<Long> ids);
}

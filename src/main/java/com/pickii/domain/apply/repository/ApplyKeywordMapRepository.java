package com.pickii.domain.apply.repository;

import com.pickii.domain.apply.entity.ApplyKeywordMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyKeywordMapRepository extends JpaRepository<ApplyKeywordMap, ApplyKeywordMap.Pk> {

    void deleteAllByApplyId(Long applyId);
}

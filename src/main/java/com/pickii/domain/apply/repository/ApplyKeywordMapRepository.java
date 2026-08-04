package com.pickii.domain.apply.repository;

import com.pickii.domain.apply.entity.ApplyKeywordMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApplyKeywordMapRepository extends JpaRepository<ApplyKeywordMap, ApplyKeywordMap.Pk> {

    void deleteAllByApplyId(Long applyId);

    List<ApplyKeywordMap> findAllByApplyIdIn(Collection<Long> applyIds);
}

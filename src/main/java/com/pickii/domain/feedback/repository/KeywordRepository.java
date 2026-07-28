package com.pickii.domain.feedback.repository;

import com.pickii.domain.feedback.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
}

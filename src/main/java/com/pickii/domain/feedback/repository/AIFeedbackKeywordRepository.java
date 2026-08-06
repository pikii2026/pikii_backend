package com.pickii.domain.feedback.repository;

import com.pickii.domain.feedback.entity.AIFeedbackKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIFeedbackKeywordRepository extends JpaRepository<AIFeedbackKeyword, AIFeedbackKeyword.Pk> {

    List<AIFeedbackKeyword> findAllByAiFeedbackId(Long aiFeedbackId);

    void deleteAllByAiFeedbackId(Long aiFeedbackId);
}

package com.pickii.domain.feedback.repository;

import com.pickii.domain.feedback.entity.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Long> {

    Optional<AIFeedback> findByProjectIdAndMemberId(Long projectId, Long memberId);

    List<AIFeedback> findAllByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}

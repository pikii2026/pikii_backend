package com.pickii.domain.feedback.repository;

import com.pickii.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByProjectIdAndRevieweeId(Long projectId, Long revieweeId);

    boolean existsByProjectIdAndReviewerIdAndRevieweeId(Long projectId, Long reviewerId, Long revieweeId);

    long countByProjectIdAndRevieweeId(Long projectId, Long revieweeId);
}

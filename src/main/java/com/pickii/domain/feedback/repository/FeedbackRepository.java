package com.pickii.domain.feedback.repository;

import com.pickii.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByProjectIdAndRevieweeId(Long projectId, Long revieweeId);

    boolean existsByProjectIdAndReviewerIdAndRevieweeId(Long projectId, Long reviewerId, Long revieweeId);

    long countByProjectIdAndRevieweeId(Long projectId, Long revieweeId);

    /** 회원 탈퇴 시 평가자(작성자)만 '알 수 없음'으로 남긴다 (DB_Schema: ReviewerId ON DELETE SET NULL) */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Feedback f SET f.reviewer = NULL WHERE f.reviewer.id = :memberId")
    void detachReviewer(@Param("memberId") Long memberId);

    /** 회원 탈퇴 시 그 회원이 평가받은 내역은 함께 삭제한다 (DB_Schema: RevieweeId ON DELETE CASCADE) */
    void deleteAllByRevieweeId(Long revieweeId);
}

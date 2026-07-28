package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByRecruitIdOrderByCreatedAtAsc(Long recruitId);

    /** 4-6 내가 작성한 댓글 조회 — 삭제된 댓글 및 삭제된 공고의 댓글은 제외 */
    @Query("SELECT c FROM Comment c JOIN FETCH c.recruit r "
            + "WHERE c.member.id = :memberId AND c.deletedAt IS NULL AND r.deletedAt IS NULL")
    Page<Comment> findMyComments(@Param("memberId") Long memberId, Pageable pageable);
}

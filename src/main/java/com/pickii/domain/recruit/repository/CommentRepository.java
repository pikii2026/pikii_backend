package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByRecruitIdOrderByCreatedAtAsc(Long recruitId);
}

package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}

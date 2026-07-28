package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    long countByIdIn(Collection<Long> ids);
}

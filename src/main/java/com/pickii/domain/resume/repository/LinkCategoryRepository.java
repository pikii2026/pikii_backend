package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.LinkCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkCategoryRepository extends JpaRepository<LinkCategory, Long> {

    Optional<LinkCategory> findByName(String name);
}

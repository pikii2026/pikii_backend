package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    long countByIdIn(Collection<Long> ids);
}

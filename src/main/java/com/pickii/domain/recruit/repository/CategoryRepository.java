package com.pickii.domain.recruit.repository;

import com.pickii.domain.recruit.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

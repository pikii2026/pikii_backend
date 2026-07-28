package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByName(String name);
}

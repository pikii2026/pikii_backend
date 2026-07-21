package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.MemberResume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberResumeRepository extends JpaRepository<MemberResume, Long> {
}

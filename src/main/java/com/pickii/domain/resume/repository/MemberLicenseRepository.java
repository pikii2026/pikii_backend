package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.MemberLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberLicenseRepository extends JpaRepository<MemberLicense, MemberLicense.Pk> {

    List<MemberLicense> findAllByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}

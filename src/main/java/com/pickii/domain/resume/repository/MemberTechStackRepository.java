package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.MemberTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberTechStackRepository extends JpaRepository<MemberTechStack, MemberTechStack.Pk> {

    List<MemberTechStack> findAllByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}

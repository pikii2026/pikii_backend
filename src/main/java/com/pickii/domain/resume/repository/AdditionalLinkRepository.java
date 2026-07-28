package com.pickii.domain.resume.repository;

import com.pickii.domain.resume.entity.AdditionalLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdditionalLinkRepository extends JpaRepository<AdditionalLink, Long> {

    @Query("select al from AdditionalLink al join fetch al.linkCategory where al.member.id = :memberId")
    List<AdditionalLink> findAllWithCategoryByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from AdditionalLink al where al.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}

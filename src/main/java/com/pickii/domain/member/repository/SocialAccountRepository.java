package com.pickii.domain.member.repository;

import com.pickii.domain.member.entity.LoginProvider;
import com.pickii.domain.member.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(LoginProvider provider, String providerId);

    List<SocialAccount> findAllByMemberId(Long memberId);

    Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, LoginProvider provider);

    /** 회원 탈퇴(1-9) 시 Member Hard Delete 전에 즉시 실행되어야 하는 선삭제(FK 제약 회피) */
    @Modifying
    @Query("delete from SocialAccount s where s.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}

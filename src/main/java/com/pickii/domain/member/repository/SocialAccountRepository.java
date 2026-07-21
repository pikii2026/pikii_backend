package com.pickii.domain.member.repository;

import com.pickii.domain.member.entity.LoginProvider;
import com.pickii.domain.member.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(LoginProvider provider, String providerId);

    List<SocialAccount> findAllByMemberId(Long memberId);

    Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, LoginProvider provider);
}

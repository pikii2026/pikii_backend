package com.pickii.domain.notification.repository;

import com.pickii.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findAllByMemberId(Long memberId);

    void deleteByFcmTokenAndMemberId(String fcmToken, Long memberId);

    void deleteByFcmToken(String fcmToken);
}

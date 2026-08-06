package com.pickii.domain.notification.repository;

import com.pickii.domain.notification.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    Page<NotificationHistory> findAllByMemberIdOrderBySentAtDesc(Long memberId, Pageable pageable);

    List<NotificationHistory> findAllByMemberIdAndIsReadFalse(Long memberId);

    long countByMemberIdAndIsReadFalse(Long memberId);

    void deleteAllByMemberId(Long memberId);
}

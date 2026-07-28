package com.pickii.domain.notification.repository;

import com.pickii.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /** 회원 탈퇴(1-9) 시 Member Hard Delete 전에 즉시 실행되어야 하는 선삭제(FK 제약 회피) */
    @Modifying
    @Query("delete from NotificationSetting n where n.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}

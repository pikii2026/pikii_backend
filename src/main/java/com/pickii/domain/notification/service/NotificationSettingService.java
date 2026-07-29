package com.pickii.domain.notification.service;

import com.pickii.domain.notification.dto.NotificationSettingRequest;
import com.pickii.domain.notification.dto.NotificationSettingResponse;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 수신 설정 조회/수정 (API_SPEC 9-5, 9-6)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    /** 9-5 알림 설정 조회 */
    public NotificationSettingResponse getSetting(Long memberId) {
        NotificationSetting setting = getSettingEntity(memberId);
        return new NotificationSettingResponse(
                setting.isChatNoti(),
                setting.isApplicantNoti(),
                setting.isCommentNoti(),
                setting.isScheduleNoti(),
                setting.isMatchNoti(),
                setting.isProjectNoti(),
                setting.isMarketingNoti()
        );
    }

    /** 9-6 알림 설정 수정 */
    @Transactional
    public void updateSetting(Long memberId, NotificationSettingRequest request) {
        NotificationSetting setting = getSettingEntity(memberId);
        setting.update(
                request.chatNoti(),
                request.applicantNoti(),
                request.commentNoti(),
                request.scheduleNoti(),
                request.matchNoti(),
                request.projectNoti(),
                request.marketingNoti()
        );
    }

    private NotificationSetting getSettingEntity(Long memberId) {
        return notificationSettingRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("알림 설정이 없는 회원입니다."));
    }
}

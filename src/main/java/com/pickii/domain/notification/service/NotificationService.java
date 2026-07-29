package com.pickii.domain.notification.service;

import com.pickii.domain.notification.dto.NotificationResponse;
import com.pickii.domain.notification.dto.UnreadCountResponse;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

/**
 * 알림 목록/읽음처리/삭제/안읽음 개수 (API_SPEC 9-1~9-4, 9-7)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    /** 9-1 알림 목록 조회 */
    public PageResponse<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {
        Page<NotificationHistory> page = notificationHistoryRepository.findAllByMemberIdOrderBySentAtDesc(memberId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    /** 9-2 알림 읽음 처리 */
    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        NotificationHistory history = getOwnedNotification(memberId, notificationId);
        history.markRead();
    }

    /** 9-3 전체 읽음 처리 */
    @Transactional
    public void markAllRead(Long memberId) {
        notificationHistoryRepository.findAllByMemberIdAndIsReadFalse(memberId)
                .forEach(NotificationHistory::markRead);
    }

    /** 9-4 알림 삭제 */
    @Transactional
    public void delete(Long memberId, Long notificationId) {
        NotificationHistory history = getOwnedNotification(memberId, notificationId);
        notificationHistoryRepository.delete(history);
    }

    /** 9-7 안 읽은 알림 개수 조회 */
    public UnreadCountResponse getUnreadCount(Long memberId) {
        return new UnreadCountResponse(notificationHistoryRepository.countByMemberIdAndIsReadFalse(memberId));
    }

    private NotificationHistory getOwnedNotification(Long memberId, Long notificationId) {
        NotificationHistory history = notificationHistoryRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!history.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return history;
    }

    private NotificationResponse toResponse(NotificationHistory history) {
        return new NotificationResponse(
                history.getId(),
                history.getType(),
                history.getTitle(),
                history.getContent(),
                history.getReferenceType(),
                history.getReferenceId(),
                history.isRead(),
                history.getSentAt().atOffset(ZoneOffset.ofHours(9)),
                history.getReadAt() == null ? null : history.getReadAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}

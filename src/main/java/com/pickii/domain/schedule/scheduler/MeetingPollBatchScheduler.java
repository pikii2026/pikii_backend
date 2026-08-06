package com.pickii.domain.schedule.scheduler;

import com.pickii.domain.schedule.service.MeetingPollService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 회의 조율 응답 마감 리마인더 배치 (API_SPEC 7-C).
 * 30분마다 실행하며, 마감 3시간 이내로 임박한 조율의 미응답 팀원에게 리마인더를 보낸다.
 */
@Component
@RequiredArgsConstructor
public class MeetingPollBatchScheduler {

    private final MeetingPollService meetingPollService;

    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void runPendingReminders() {
        meetingPollService.sendPendingReminders();
    }
}

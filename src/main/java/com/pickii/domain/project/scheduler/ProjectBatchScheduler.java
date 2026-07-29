package com.pickii.domain.project.scheduler;

import com.pickii.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 종료 정책 배치 (API_SPEC 6-4 종료 정책).
 * 매일 자정: 진행기간이 끝난 프로젝트에 종료 확인 알림 발송 → 3일 무응답 시 자동 종료.
 */
@Component
@RequiredArgsConstructor
public class ProjectBatchScheduler {

    private final ProjectService projectService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runEndCheckPolicy() {
        projectService.sendEndCheckNotifications();
        projectService.autoCloseUnrespondedProjects();
    }
}

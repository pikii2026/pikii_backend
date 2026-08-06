package com.pickii.domain.feedback.scheduler;

import com.pickii.domain.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 피드백 생성 배치 (API_SPEC 4-11/4-12).
 * 매일 자정: 평가 기간(3일)이 지났는데 조기 완료 트리거로 생성되지 않은 AI 피드백을 일괄 생성한다.
 */
@Component
@RequiredArgsConstructor
public class FeedbackBatchScheduler {

    private final FeedbackService feedbackService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runPendingAiFeedbackGeneration() {
        feedbackService.generatePendingAiFeedbackBatch();
    }
}

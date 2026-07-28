package com.pickii.domain.recruit.dto;

import com.pickii.domain.recruit.entity.Topic;

/**
 * API_SPEC 5-2 주제 조회 응답 항목
 */
public record TopicResponse(
        Long topicId,
        String name
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.getId(), topic.getName());
    }
}

package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingPollAvailabilityRepository extends JpaRepository<MeetingPollAvailability, MeetingPollAvailability.Pk> {

    List<MeetingPollAvailability> findAllBySlotIdIn(List<Long> slotIds);

    long countBySlotId(Long slotId);

    void deleteAllByMemberId(Long memberId);

    /** 1-9 회원 탈퇴 시, 본인이 개설한 조율을 통째로 정리할 때 슬롯별 응답도 함께 삭제한다. */
    void deleteAllBySlotIdIn(List<Long> slotIds);
}

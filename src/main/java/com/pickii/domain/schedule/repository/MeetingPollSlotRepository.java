package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingPollSlotRepository extends JpaRepository<MeetingPollSlot, Long> {

    List<MeetingPollSlot> findAllByPollIdOrderByStartAt(Long pollId);

    /** 1-9 회원 탈퇴 시, 본인이 개설한 조율의 슬롯을 정리하기 위해 슬롯 ID를 먼저 조회한다 (Availability 정리용). */
    @Query("SELECT s.id FROM MeetingPollSlot s WHERE s.poll.id IN :pollIds")
    List<Long> findIdsByPollIdIn(@Param("pollIds") List<Long> pollIds);

    void deleteAllByPollIdIn(List<Long> pollIds);
}

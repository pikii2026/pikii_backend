package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPoll;
import com.pickii.domain.schedule.entity.MeetingPollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingPollRepository extends JpaRepository<MeetingPoll, Long> {

    /** 프로젝트당 COLLECTING 조율은 1개만 존재 */
    Optional<MeetingPoll> findByProjectIdAndStatus(Long projectId, MeetingPollStatus status);

    boolean existsByProjectIdAndStatus(Long projectId, MeetingPollStatus status);

    /** 7-18 팀 일정 삭제 시, 조율로 확정된 일정이면 연결을 해제하기 위해 조회 */
    Optional<MeetingPoll> findByScheduleId(Long scheduleId);

    /**
     * 1-9 회원 탈퇴 시, 본인이 개설한 조율(CreatedBy)을 정리하기 위해 먼저 대상 ID를 조회한다.
     * (DB_Schema: MeetingPoll.CreatedBy는 ON DELETE CASCADE 대상)
     */
    @Query("SELECT p.id FROM MeetingPoll p WHERE p.createdBy.id = :memberId")
    List<Long> findIdsByCreatedById(@Param("memberId") Long memberId);

    void deleteAllByCreatedById(Long memberId);

    /** 응답 마감이 3시간 이내로 임박했고(아직 지나지는 않음) 아직 리마인더를 안 보낸 진행 중인 조율 */
    @Query("SELECT p FROM MeetingPoll p WHERE p.status = :status "
            + "AND p.deadline > :now AND p.deadline <= :threshold AND p.reminderSentAt IS NULL")
    List<MeetingPoll> findAllPendingReminders(
            @Param("status") MeetingPollStatus status,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);
}

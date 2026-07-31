package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingPollMemberRepository extends JpaRepository<MeetingPollMember, MeetingPollMember.Pk> {

    long countByPollId(Long pollId);

    long countByPollIdAndRespondedTrue(Long pollId);

    List<MeetingPollMember> findAllByPollIdAndRespondedFalse(Long pollId);

    void deleteAllByMemberId(Long memberId);

    /** 1-9 회원 탈퇴 시, 본인이 개설한 조율을 통째로 정리할 때 참여자 응답 여부 레코드도 함께 삭제한다. */
    void deleteAllByPollIdIn(List<Long> pollIds);
}

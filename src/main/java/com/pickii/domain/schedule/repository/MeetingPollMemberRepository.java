package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingPollMemberRepository extends JpaRepository<MeetingPollMember, MeetingPollMember.Pk> {

    long countByPollId(Long pollId);

    long countByPollIdAndRespondedTrue(Long pollId);

    List<MeetingPollMember> findAllByPollIdAndRespondedFalse(Long pollId);

    void deleteAllByMemberId(Long memberId);
}

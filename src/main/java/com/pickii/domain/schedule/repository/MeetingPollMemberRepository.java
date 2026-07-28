package com.pickii.domain.schedule.repository;

import com.pickii.domain.schedule.entity.MeetingPollMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingPollMemberRepository extends JpaRepository<MeetingPollMember, MeetingPollMember.Pk> {

    long countByPollId(Long pollId);

    long countByPollIdAndRespondedTrue(Long pollId);
}

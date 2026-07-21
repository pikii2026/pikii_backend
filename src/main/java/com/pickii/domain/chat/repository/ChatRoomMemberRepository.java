package com.pickii.domain.chat.repository;

import com.pickii.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findAllByMemberId(Long memberId);

    List<ChatRoomMember> findAllByChatRoomId(Long chatRoomId);

    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
}

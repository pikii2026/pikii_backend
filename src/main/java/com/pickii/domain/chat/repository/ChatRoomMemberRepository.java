package com.pickii.domain.chat.repository;

import com.pickii.domain.chat.entity.ChatRoom;
import com.pickii.domain.chat.entity.ChatRoomMember;
import com.pickii.domain.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findAllByMemberId(Long memberId);

    List<ChatRoomMember> findAllByMemberIdAndChatRoom_Type(Long memberId, ChatRoomType type);

    List<ChatRoomMember> findAllByChatRoomId(Long chatRoomId);

    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    /** 8-5: 두 사용자가 공통으로 속한 DIRECT 채팅방 조회 */
    @Query("SELECT crm1.chatRoom FROM ChatRoomMember crm1 JOIN ChatRoomMember crm2 "
            + "ON crm1.chatRoom = crm2.chatRoom "
            + "WHERE crm1.member.id = :a AND crm2.member.id = :b AND crm1.chatRoom.type = 'DIRECT'")
    Optional<ChatRoom> findDirectRoomBetween(@Param("a") Long a, @Param("b") Long b);
}

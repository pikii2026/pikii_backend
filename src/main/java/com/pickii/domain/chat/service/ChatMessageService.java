package com.pickii.domain.chat.service;

import com.pickii.domain.chat.document.ChatMessage;
import com.pickii.domain.chat.dto.ChatMessageResponse;
import com.pickii.domain.chat.dto.ChatMessageSendRequest;
import com.pickii.domain.chat.repository.ChatMessageRepository;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

/**
 * 실시간 채팅 메시지 전송(WebSocket, API_SPEC 8장 도입부).
 * 조회/방 관리는 REST({@link ChatRoomService})가, 실제 메시지 저장/브로드캐스트는 여기서 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ChatMessageResponse send(Long memberId, Long chatRoomId, ChatMessageSendRequest request) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .memberId(memberId)
                .type(request.type())
                .message(request.message())
                .imageUrl(request.imageUrl())
                .build());

        return new ChatMessageResponse(
                saved.getId(),
                memberId,
                sender.getNickname(),
                saved.getType(),
                saved.getMessage(),
                saved.getImageUrl(),
                saved.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }
}

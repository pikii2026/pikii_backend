package com.pickii.domain.chat.service;

import com.pickii.domain.chat.document.ChatMessage;
import com.pickii.domain.chat.dto.ChatMessageResponse;
import com.pickii.domain.chat.dto.ChatMessageSendRequest;
import com.pickii.domain.chat.entity.ChatMessageType;
import com.pickii.domain.chat.entity.ChatRoomMember;
import com.pickii.domain.chat.repository.ChatMessageRepository;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.event.NotificationCreatedEvent;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

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

        chatRoomMemberRepository.findAllByChatRoomId(chatRoomId).stream()
                .filter(crm -> !crm.getMember().getId().equals(memberId))
                .forEach(crm -> notify(crm, chatRoomId, sender.getNickname()));

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

    /**
     * 회의 조율/팀 일정 등 스케줄 도메인 이벤트를 그룹 채팅방에 시스템 메시지로 안내한다.
     * 작성자가 없는 메시지이므로 senderId/senderNickname은 null로 보낸다.
     */
    @Transactional
    public void sendSystemMessage(Long chatRoomId, String content) {
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .memberId(null)
                .type(ChatMessageType.SYSTEM)
                .message(content)
                .build());

        ChatMessageResponse response = new ChatMessageResponse(
                saved.getId(),
                null,
                null,
                saved.getType(),
                saved.getMessage(),
                saved.getImageUrl(),
                saved.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatRoomId, response);
    }

    private void notify(ChatRoomMember recipient, Long chatRoomId, String senderNickname) {
        if (!recipient.isNotiEnabled()) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(recipient.getMember().getId()).orElse(null);
        if (setting == null || !setting.isChatNoti()) {
            return;
        }
        String title = "새 메시지가 도착했습니다.";
        String content = senderNickname + "님이 메시지를 보냈습니다.";
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(recipient.getMember())
                .title(title)
                .content(content)
                .type(NotificationType.CHAT)
                .referenceType(NotificationReferenceType.CHATROOM)
                .referenceId(chatRoomId)
                .build());
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                recipient.getMember().getId(), title, content,
                NotificationType.CHAT, NotificationReferenceType.CHATROOM, chatRoomId));
    }
}

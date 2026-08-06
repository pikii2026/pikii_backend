package com.pickii.domain.chat.controller;

import com.pickii.domain.chat.dto.ChatMessageResponse;
import com.pickii.domain.chat.dto.ChatMessageSendRequest;
import com.pickii.domain.chat.dto.ChatRoomReadRequest;
import com.pickii.domain.chat.service.ChatMessageService;
import com.pickii.domain.chat.service.ChatRoomService;
import com.pickii.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 실시간 채팅 WebSocket(STOMP) 엔드포인트 (API_SPEC 8장 도입부: "실시간 채팅은 WebSocket을 사용한다").
 * 클라이언트 발행: /pub/chatrooms/{chatRoomId}/messages, /pub/chatrooms/{chatRoomId}/read
 * 구독: /sub/chatrooms/{chatRoomId}, 에러 알림: /user/queue/errors
 */
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chatrooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            @Payload ChatMessageSendRequest request,
                            Principal principal) {
        Long memberId = memberId(principal);
        try {
            ChatMessageResponse response = chatMessageService.send(memberId, chatRoomId, request);
            messagingTemplate.convertAndSend("/sub/chatrooms/" + chatRoomId, response);
        } catch (BusinessException e) {
            sendError(principal, e);
        }
    }

    @MessageMapping("/chatrooms/{chatRoomId}/read")
    public void markRead(@DestinationVariable Long chatRoomId,
                         @Payload ChatRoomReadRequest request,
                         Principal principal) {
        Long memberId = memberId(principal);
        try {
            chatRoomService.markRead(memberId, chatRoomId, request.lastReadMessageId());
        } catch (BusinessException e) {
            sendError(principal, e);
        }
    }

    private void sendError(Principal principal, BusinessException e) {
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                Map.of("code", e.getErrorCode().name(), "message", e.getMessage()));
    }

    private Long memberId(Principal principal) {
        return Long.parseLong(principal.getName());
    }
}

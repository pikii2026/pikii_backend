package com.pickii.domain.chat.service;

import com.pickii.domain.chat.document.ChatMessage;
import com.pickii.domain.chat.dto.ChatMessagePageResponse;
import com.pickii.domain.chat.dto.ChatMessageResponse;
import com.pickii.domain.chat.dto.ChatRoomDetailResponse;
import com.pickii.domain.chat.dto.ChatRoomListItem;
import com.pickii.domain.chat.dto.ChatRoomMemberSummary;
import com.pickii.domain.chat.dto.DirectChatRoomResponse;
import com.pickii.domain.chat.entity.ChatRoom;
import com.pickii.domain.chat.entity.ChatRoomMember;
import com.pickii.domain.chat.entity.ChatRoomType;
import com.pickii.domain.chat.repository.ChatMessageRepository;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.service.ProjectService;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채팅방 목록/상세/생성/읽음/나가기/알림설정 (API_SPEC 8-1~8-3, 8-5~8-8)
 * 이미지 업로드(8-4)는 {@link ChatImageService} 참고.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;

    /** 8-1 채팅방 목록 */
    public PageResponse<ChatRoomListItem> getChatRooms(Long memberId, ChatRoomType type, Pageable pageable) {
        List<ChatRoomMember> memberships = type == null
                ? chatRoomMemberRepository.findAllByMemberId(memberId)
                : chatRoomMemberRepository.findAllByMemberIdAndChatRoom_Type(memberId, type);

        List<ChatRoomListItem> items = memberships.stream()
                .map(membership -> toListItem(membership, memberId))
                .sorted(Comparator.comparing(ChatRoomListItem::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = items.size();
        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        Page<ChatRoomListItem> page = new PageImpl<>(items.subList(fromIndex, toIndex), pageable, total);
        return PageResponse.from(page);
    }

    private ChatRoomListItem toListItem(ChatRoomMember membership, Long memberId) {
        ChatRoom room = membership.getChatRoom();
        String title = resolveTitle(room, memberId);
        Optional<ChatMessage> lastMessage = chatMessageRepository
                .findAllByChatRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, 1))
                .getContent().stream().findFirst();
        long unreadCount = countUnread(room.getId(), membership.getLastReadMessageId());
        Long projectId = room.getProject() != null ? room.getProject().getId() : null;

        return new ChatRoomListItem(
                room.getId(),
                room.getType(),
                title,
                projectId,
                lastMessage.map(ChatMessage::getMessage).orElse(null),
                lastMessage.map(m -> m.getCreatedAt().atOffset(ZoneOffset.ofHours(9))).orElse(null),
                unreadCount,
                membership.isNotiEnabled()
        );
    }

    /** 8-2 채팅방 상세 */
    public ChatRoomDetailResponse getDetail(Long memberId, Long chatRoomId) {
        ChatRoom room = getChatRoom(chatRoomId);
        requireMember(chatRoomId, memberId);

        List<ChatRoomMemberSummary> members = chatRoomMemberRepository.findAllByChatRoomId(chatRoomId).stream()
                .map(m -> new ChatRoomMemberSummary(m.getMember().getId(), m.getMember().getNickname(), m.getMember().getExp()))
                .toList();
        String title = resolveTitle(room, memberId);

        if (room.getType() != ChatRoomType.GROUP || room.getProject() == null) {
            return new ChatRoomDetailResponse(room.getId(), title, members, null, null, null, null, null);
        }

        Project project = room.getProject();
        Boolean isLeader = projectMemberRepository
                .findByProjectIdAndMemberIdAndLeftAtIsNull(project.getId(), memberId)
                .map(ProjectMember::isLeader)
                .orElse(false);
        return new ChatRoomDetailResponse(
                room.getId(), title, members,
                project.getId(), project.getStartDate(), project.getEndDate(), project.getStatus(), isLeader
        );
    }

    /** 8-3 이전 채팅 조회 */
    public ChatMessagePageResponse getMessages(Long memberId, Long chatRoomId, String cursor, int size) {
        getChatRoom(chatRoomId);
        requireMember(chatRoomId, memberId);

        Pageable pageable = PageRequest.of(0, size);
        Page<ChatMessage> page = cursor == null
                ? chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable)
                : chatMessageRepository.findAllByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(chatRoomId, cursor, pageable);

        List<ChatMessage> messages = page.getContent();
        Set<Long> senderIds = messages.stream()
                .map(ChatMessage::getMemberId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Member> senders = memberRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));

        List<ChatMessageResponse> content = messages.stream()
                .map(m -> toMessageResponse(m, senders))
                .toList();
        String nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        return new ChatMessagePageResponse(content, nextCursor, page.hasNext());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, Map<Long, Member> senders) {
        Member sender = message.getMemberId() == null ? null : senders.get(message.getMemberId());
        String nickname = sender == null ? "알 수 없음" : sender.getNickname();
        int exp = sender == null ? 0 : sender.getExp();
        return new ChatMessageResponse(
                message.getId(),
                message.getMemberId(),
                nickname,
                exp,
                message.getType(),
                message.getMessage(),
                message.getImageUrl(),
                message.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    /** 8-5 1:1 채팅방 생성 */
    @Transactional
    public DirectChatRoomResponse createOrGetDirectRoom(Long memberId, Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHAT_SELF);
        }
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<ChatRoom> existing = chatRoomMemberRepository.findDirectRoomBetween(memberId, targetMemberId);
        if (existing.isPresent()) {
            return new DirectChatRoomResponse(existing.get().getId(), ChatRoomType.DIRECT, false);
        }

        Member me = memberRepository.getReferenceById(memberId);
        ChatRoom room = new ChatRoom(ChatRoomType.DIRECT, null);
        chatRoomRepository.save(room);
        chatRoomMemberRepository.save(new ChatRoomMember(room, me));
        chatRoomMemberRepository.save(new ChatRoomMember(room, target));
        return new DirectChatRoomResponse(room.getId(), ChatRoomType.DIRECT, true);
    }

    /** 8-6 채팅방 읽음 처리 */
    @Transactional
    public void markRead(Long memberId, Long chatRoomId, String lastReadMessageId) {
        getChatRoom(chatRoomId);
        ChatRoomMember membership = requireMember(chatRoomId, memberId);
        membership.updateReadCursor(lastReadMessageId);
    }

    /** 8-7 채팅방 나가기 (GROUP은 프로젝트 나가기(6-6)와 동일 동작) */
    @Transactional
    public void leave(Long memberId, Long chatRoomId) {
        ChatRoom room = getChatRoom(chatRoomId);
        ChatRoomMember membership = requireMember(chatRoomId, memberId);

        if (room.getType() == ChatRoomType.GROUP) {
            projectService.leave(memberId, room.getProject().getId());
        } else {
            chatRoomMemberRepository.delete(membership);
        }
    }

    /** 8-8 채팅방 알림 설정 */
    @Transactional
    public void updateNotification(Long memberId, Long chatRoomId, boolean enabled) {
        getChatRoom(chatRoomId);
        ChatRoomMember membership = requireMember(chatRoomId, memberId);
        membership.toggleNoti(enabled);
    }

    private long countUnread(Long chatRoomId, String lastReadMessageId) {
        return lastReadMessageId == null
                ? chatMessageRepository.countByChatRoomId(chatRoomId)
                : chatMessageRepository.countByChatRoomIdAndIdGreaterThan(chatRoomId, lastReadMessageId);
    }

    private String resolveTitle(ChatRoom room, Long memberId) {
        if (room.getType() == ChatRoomType.GROUP) {
            return room.getProject().getName();
        }
        return chatRoomMemberRepository.findAllByChatRoomId(room.getId()).stream()
                .filter(m -> !m.getMember().getId().equals(memberId))
                .findFirst()
                .map(m -> m.getMember().getNickname())
                .orElse("알 수 없음");
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    private ChatRoomMember requireMember(Long chatRoomId, Long memberId) {
        return chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }
}

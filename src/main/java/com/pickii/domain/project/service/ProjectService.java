package com.pickii.domain.project.service;

import com.pickii.domain.apply.entity.Apply;
import com.pickii.domain.apply.entity.ApplyStatus;
import com.pickii.domain.apply.repository.ApplyRepository;
import com.pickii.domain.chat.entity.ChatRoom;
import com.pickii.domain.chat.entity.ChatRoomMember;
import com.pickii.domain.chat.entity.ChatRoomType;
import com.pickii.domain.chat.repository.ChatRoomMemberRepository;
import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.domain.project.dto.LeaderDelegateRequest;
import com.pickii.domain.project.dto.ProjectCreateRequest;
import com.pickii.domain.project.dto.ProjectCreateResponse;
import com.pickii.domain.project.dto.ProjectDetailResponse;
import com.pickii.domain.project.dto.ProjectExtendRequest;
import com.pickii.domain.project.dto.ProjectExtendResponse;
import com.pickii.domain.project.dto.ProjectMemberResponse;
import com.pickii.domain.project.dto.ProjectStatusResponse;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.domain.recruit.entity.Recruit;
import com.pickii.domain.recruit.repository.RecruitRepository;
import com.pickii.global.common.response.PageResponse;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 프로젝트 생성 (API_SPEC 6-1), 상세/팀원 조회 (6-2, 6-3), 종료/연장 (6-4, 6-5),
 * 나가기/퇴출 (6-6, 6-7), 프로젝트장 위임 (6-8), 상태 조회 (6-9)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RecruitRepository recruitRepository;
    private final ApplyRepository applyRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    /** 6-1 프로젝트 생성 (그룹 채팅 생성) */
    @Transactional
    public ProjectCreateResponse createProject(Long memberId, Long recruitId, ProjectCreateRequest request) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUIT_NOT_FOUND));
        if (recruit.getMember() == null || !recruit.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (projectRepository.existsByRecruitId(recruitId)) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_EXISTS);
        }
        List<Apply> accepted = applyRepository.findAllByRecruitId(recruitId).stream()
                .filter(apply -> apply.getStatus() == ApplyStatus.ACCEPTED)
                .filter(apply -> apply.getMember() != null && !apply.getMember().getId().equals(memberId))
                .toList();
        if (accepted.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ACCEPTED_APPLICANT);
        }

        Project project = new Project(recruit, request.name(), recruit.getStartDate(), recruit.getEndDate());
        projectRepository.save(project);

        ChatRoom chatRoom = new ChatRoom(ChatRoomType.GROUP, project);
        chatRoomRepository.save(chatRoom);

        Member author = recruit.getMember();
        projectMemberRepository.save(new ProjectMember(project, author, true));
        chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, author));

        for (Apply apply : accepted) {
            Member applicant = apply.getMember();
            projectMemberRepository.save(new ProjectMember(project, applicant, false));
            chatRoomMemberRepository.save(new ChatRoomMember(chatRoom, applicant));
            notify(applicant, project, "프로젝트가 생성되었습니다.", "'" + project.getName() + "' 프로젝트에 참여하게 되었습니다.");
        }

        int memberCount = accepted.size() + 1;
        return new ProjectCreateResponse(project.getId(), chatRoom.getId(), recruit.getStatus(), memberCount);
    }

    /** 6-2 프로젝트 상세 조회 */
    public ProjectDetailResponse getDetail(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        requireActiveMember(projectId, memberId);
        Long leaderId = findLeader(projectId).getMember().getId();
        return new ProjectDetailResponse(
                project.getId(),
                project.getRecruit().getTitle(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                leaderId
        );
    }

    /** 6-3 프로젝트 팀원 조회 */
    public PageResponse<ProjectMemberResponse> getMembers(Long memberId, Long projectId, Pageable pageable) {
        getProject(projectId);
        requireActiveMember(projectId, memberId);
        Page<ProjectMember> page = projectMemberRepository.findByProjectIdAndLeftAtIsNull(projectId, pageable);
        return PageResponse.from(page, this::toMemberResponse);
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember pm) {
        return new ProjectMemberResponse(
                pm.getMember().getId(),
                pm.getMember().getNickname(),
                pm.isLeader(),
                pm.getJoinedAt().atOffset(ZoneOffset.ofHours(9))
        );
    }

    /** 6-4 프로젝트 종료 */
    @Transactional
    public void close(Long memberId, Long projectId) {
        Project project = getLeaderOwnedProject(memberId, projectId);
        if (project.isEnded()) {
            throw new BusinessException(ErrorCode.ALREADY_ENDED);
        }
        project.end();
        notifyAllMembers(project, "프로젝트가 종료되었습니다.", "'" + project.getName() + "' 프로젝트가 종료되어 상호평가를 진행할 수 있습니다.");
    }

    /** 6-5 프로젝트 진행기간 연장 */
    @Transactional
    public ProjectExtendResponse extend(Long memberId, Long projectId, ProjectExtendRequest request) {
        Project project = getLeaderOwnedProject(memberId, projectId);
        if (project.isEnded()) {
            throw new BusinessException(ErrorCode.ALREADY_ENDED);
        }
        if (request.endDate().isBefore(project.getEndDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "새로운 종료일은 기존 종료일 이후여야 합니다.");
        }
        project.extend(request.endDate());
        notifyAllMembers(project, "프로젝트 기간이 연장되었습니다.", "'" + project.getName() + "' 프로젝트가 " + request.endDate() + "까지 연장되었습니다.");
        return new ProjectExtendResponse(project.getId(), project.getEndDate(), project.getStatus());
    }

    /** 6-6 프로젝트 나가기 */
    @Transactional
    public void leave(Long memberId, Long projectId) {
        getProject(projectId);
        ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (pm.isLeader()) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        leaveInternal(pm);
    }

    /** 6-7 팀원 퇴출 */
    @Transactional
    public void kick(Long memberId, Long projectId, Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }
        getLeaderOwnedProject(memberId, projectId);
        ProjectMember target = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
        Project project = target.getProject();
        leaveInternal(target);
        notify(target.getMember(), project, "프로젝트에서 퇴출되었습니다.", "'" + project.getName() + "' 프로젝트에서 퇴출되었습니다.");
    }

    private void leaveInternal(ProjectMember pm) {
        pm.leave();
        ChatRoom groupChatRoom = chatRoomRepository.findByProjectId(pm.getProject().getId())
                .orElseThrow(() -> new IllegalStateException("GROUP 채팅방이 없는 프로젝트입니다."));
        chatRoomMemberRepository.findByChatRoomIdAndMemberId(groupChatRoom.getId(), pm.getMember().getId())
                .ifPresent(chatRoomMemberRepository::delete);
        pm.getProject().getRecruit().decreaseCurrentCount();
    }

    /** 6-8 프로젝트장 위임 */
    @Transactional
    public void delegateLeader(Long memberId, Long projectId, LeaderDelegateRequest request) {
        getLeaderOwnedProject(memberId, projectId);
        ProjectMember currentLeader = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        ProjectMember newLeader = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, request.newLeaderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
        currentLeader.delegateLeader();
        newLeader.promoteLeader();
    }

    /** 6-9 프로젝트 상태 조회 */
    public ProjectStatusResponse getStatus(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        requireActiveMember(projectId, memberId);
        return new ProjectStatusResponse(project.getStatus(), calculateProgressRate(project), project.getEndDate());
    }

    private int calculateProgressRate(Project project) {
        if (project.isEnded()) {
            return 100;
        }
        LocalDate start = project.getStartDate();
        LocalDate end = project.getEndDate();
        long totalDays = ChronoUnit.DAYS.between(start, end);
        if (totalDays <= 0) {
            return 100;
        }
        long elapsedDays = ChronoUnit.DAYS.between(start, LocalDate.now());
        double rate = (double) elapsedDays / totalDays * 100;
        return (int) Math.min(100, Math.max(0, rate));
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void requireActiveMember(Long projectId, Long memberId) {
        if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private ProjectMember findLeader(Long projectId) {
        return projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId).stream()
                .filter(ProjectMember::isLeader)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("리더가 없는 프로젝트입니다."));
    }

    private Project getLeaderOwnedProject(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!pm.isLeader()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return project;
    }

    private void notifyAllMembers(Project project, String title, String content) {
        projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId())
                .forEach(pm -> notify(pm.getMember(), project, title, content));
    }

    private void notify(Member member, Project project, String title, String content) {
        if (member == null) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(member.getId()).orElse(null);
        if (setting == null || !setting.isProjectNoti()) {
            return;
        }
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(member)
                .title(title)
                .content(content)
                .type(NotificationType.PROJECT)
                .referenceType(NotificationReferenceType.PROJECT)
                .referenceId(project.getId())
                .build());
    }
}

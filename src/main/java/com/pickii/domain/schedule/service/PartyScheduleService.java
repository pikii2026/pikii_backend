package com.pickii.domain.schedule.service;

import com.pickii.domain.chat.repository.ChatRoomRepository;
import com.pickii.domain.chat.service.ChatMessageService;
import com.pickii.domain.member.entity.Member;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.notification.entity.NotificationHistory;
import com.pickii.domain.notification.entity.NotificationReferenceType;
import com.pickii.domain.notification.entity.NotificationSetting;
import com.pickii.domain.notification.entity.NotificationType;
import com.pickii.domain.notification.repository.NotificationHistoryRepository;
import com.pickii.domain.notification.repository.NotificationSettingRepository;
import com.pickii.domain.project.entity.Project;
import com.pickii.domain.project.entity.ProjectMember;
import com.pickii.domain.project.repository.ProjectMemberRepository;
import com.pickii.domain.project.repository.ProjectRepository;
import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.dto.PartyScheduleRecurringRequest;
import com.pickii.domain.schedule.dto.PartyScheduleSingleRequest;
import com.pickii.domain.schedule.dto.PartyScheduleUpdateRequest;
import com.pickii.domain.schedule.dto.ScheduleCategoryResponse;
import com.pickii.domain.schedule.dto.ScheduleCreateResponse;
import com.pickii.domain.schedule.entity.MeetingPoll;
import com.pickii.domain.schedule.entity.PartySchedule;
import com.pickii.domain.schedule.entity.PartyScheduleAttendance;
import com.pickii.domain.schedule.entity.ProjectScheduleCategory;
import com.pickii.domain.schedule.entity.ScheduleCategory;
import com.pickii.domain.schedule.repository.MeetingPollRepository;
import com.pickii.domain.schedule.repository.PartyScheduleAttendanceRepository;
import com.pickii.domain.schedule.repository.PartyScheduleRepository;
import com.pickii.domain.schedule.repository.ProjectScheduleCategoryRepository;
import com.pickii.domain.schedule.repository.ScheduleCategoryRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 팀 일정 조회/등록/수정/삭제, 팀원별 색상 지정 (API_SPEC 7-15 ~ 7-19)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyScheduleService {

    private final PartyScheduleRepository partyScheduleRepository;
    private final PartyScheduleAttendanceRepository partyScheduleAttendanceRepository;
    private final MeetingPollRepository meetingPollRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectScheduleCategoryRepository projectScheduleCategoryRepository;
    private final ScheduleCategoryRepository scheduleCategoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageService chatMessageService;

    /** 7-15 팀 일정 조회 */
    public List<MyScheduleResponse> getMonthlySchedules(Long memberId, Long projectId, int year, int month) {
        getProject(projectId);
        requireActiveMember(projectId, memberId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate rangeStart = yearMonth.atDay(1);
        LocalDate rangeEnd = yearMonth.atEndOfMonth();

        List<PartySchedule> schedules = partyScheduleRepository
                .findAllByProjectIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(projectId, rangeEnd, rangeStart);

        ScheduleCategoryResponse myCategory = resolveMyCategory(memberId, projectId);

        return schedules.stream()
                .map(schedule -> toResponse(schedule, myCategory))
                .toList();
    }

    /** 7-16 팀 일정 직접 등록 (단발) */
    @Transactional
    public ScheduleCreateResponse createSingle(Long memberId, Long projectId, PartyScheduleSingleRequest request) {
        Project project = getLeaderOwnedProject(memberId, projectId);
        validateTimeRange(request.startTime(), request.endTime());

        PartySchedule schedule = PartySchedule.builder()
                .project(project)
                .startDate(request.date())
                .endDate(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .rrule(null)
                .exDate(null)
                .title(request.title())
                .content(request.content())
                .build();
        partyScheduleRepository.save(schedule);
        applyToAllMemberCalendars(project, schedule);

        return new ScheduleCreateResponse(schedule.getId());
    }

    /** 7-16 팀 일정 직접 등록 (반복) */
    @Transactional
    public ScheduleCreateResponse createRecurring(Long memberId, Long projectId, PartyScheduleRecurringRequest request) {
        Project project = getLeaderOwnedProject(memberId, projectId);
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 시작일은 종료일보다 이전이어야 합니다.");
        }
        validateTimeRange(request.startTime(), request.endTime());
        validateRrule(request.rrule());

        PartySchedule schedule = PartySchedule.builder()
                .project(project)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .rrule(request.rrule())
                .exDate(null)
                .title(request.title())
                .content(request.content())
                .build();
        partyScheduleRepository.save(schedule);
        applyToAllMemberCalendars(project, schedule);

        return new ScheduleCreateResponse(schedule.getId());
    }

    /** 7-17 팀 일정 수정 */
    @Transactional
    public void update(Long memberId, Long scheduleId, PartyScheduleUpdateRequest request) {
        PartySchedule schedule = getSchedule(scheduleId);
        requireActiveMember(schedule.getProject().getId(), memberId);
        validateTimeRange(request.startTime(), request.endTime());

        if (schedule.isRecurring()) {
            if (request.startDate() == null || request.endDate() == null || request.rrule() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 일정은 startDate/endDate/rrule이 필요합니다.");
            }
            if (request.startDate().isAfter(request.endDate())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 시작일은 종료일보다 이전이어야 합니다.");
            }
            validateRrule(request.rrule());
            schedule.update(request.title(), request.startDate(), request.endDate(),
                    request.startTime(), request.endTime(), request.rrule(), request.content());
        } else {
            if (request.date() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "단발 일정은 date가 필요합니다.");
            }
            schedule.update(request.title(), request.date(), request.date(),
                    request.startTime(), request.endTime(), null, request.content());
        }
    }

    /** 7-18 팀 일정 삭제. 회의 조율로 확정된 일정이면 해당 조율도 취소 처리한다. */
    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        PartySchedule schedule = getSchedule(scheduleId);
        requireActiveMember(schedule.getProject().getId(), memberId);

        meetingPollRepository.findByScheduleId(scheduleId).ifPresent(MeetingPoll::cancel);
        partyScheduleAttendanceRepository.deleteByScheduleId(scheduleId);
        partyScheduleRepository.delete(schedule);
    }

    /** 7-20 회의 참석 / 불참 변경. 회의 삭제와 무관하며 불참자가 있어도 일정 자체는 유지된다. */
    @Transactional
    public void updateAttendance(Long memberId, Long scheduleId, boolean attending) {
        PartySchedule schedule = getSchedule(scheduleId);
        requireActiveMember(schedule.getProject().getId(), memberId);

        PartyScheduleAttendance.Pk pk = new PartyScheduleAttendance.Pk(scheduleId, memberId);
        partyScheduleAttendanceRepository.findById(pk)
                .ifPresentOrElse(
                        existing -> existing.changeAttending(attending),
                        () -> {
                            PartyScheduleAttendance attendance = new PartyScheduleAttendance(scheduleId, memberId);
                            attendance.changeAttending(attending);
                            partyScheduleAttendanceRepository.save(attendance);
                        }
                );

        String nickname = memberRepository.findById(memberId).map(Member::getNickname).orElse("알 수 없음");
        announceToGroupChat(schedule.getProject().getId(),
                nickname + "님이 '" + schedule.getTitle() + "' 일정에 " + (attending ? "참석" : "불참") + "으로 표시했습니다.");
    }

    /** 7-19 프로젝트 색상(카테고리) 지정 - 개인별 */
    @Transactional
    public void selectCategory(Long memberId, Long projectId, Long categoryId) {
        getProject(projectId);
        requireActiveMember(projectId, memberId);
        ScheduleCategory category = scheduleCategoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_CATEGORY_NOT_FOUND));

        List<Long> myCategoryIds = scheduleCategoryRepository.findAllByMemberId(memberId).stream()
                .map(ScheduleCategory::getId)
                .toList();
        projectScheduleCategoryRepository.findByProjectIdAndScIdIn(projectId, myCategoryIds)
                .filter(existing -> !existing.getScId().equals(category.getId()))
                .ifPresent(projectScheduleCategoryRepository::delete);

        ProjectScheduleCategory.Pk pk = new ProjectScheduleCategory.Pk(category.getId(), projectId);
        if (!projectScheduleCategoryRepository.existsById(pk)) {
            projectScheduleCategoryRepository.save(new ProjectScheduleCategory(category.getId(), projectId));
        }
    }

    private void applyToAllMemberCalendars(Project project, PartySchedule schedule) {
        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(project.getId());
        members.forEach(pm -> {
            partyScheduleAttendanceRepository.save(new PartyScheduleAttendance(schedule.getId(), pm.getMember().getId()));
            notify(pm.getMember(), project, "새로운 팀 일정이 등록되었습니다.", "'" + schedule.getTitle() + "' 일정이 등록되었습니다.");
        });
    }

    /** 팀원이 이 프로젝트에 지정해둔 개인 카테고리(있으면)를 찾는다. */
    private ScheduleCategoryResponse resolveMyCategory(Long memberId, Long projectId) {
        List<Long> myCategoryIds = scheduleCategoryRepository.findAllByMemberId(memberId).stream()
                .map(ScheduleCategory::getId)
                .toList();
        if (myCategoryIds.isEmpty()) {
            return null;
        }
        return projectScheduleCategoryRepository.findByProjectIdAndScIdIn(projectId, myCategoryIds)
                .flatMap(mapping -> scheduleCategoryRepository.findById(mapping.getScId()))
                .map(ScheduleCategoryResponse::from)
                .orElse(null);
    }

    private MyScheduleResponse toResponse(PartySchedule schedule, ScheduleCategoryResponse category) {
        boolean recurring = schedule.isRecurring();
        return new MyScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                recurring,
                recurring ? null : schedule.getStartDate(),
                recurring ? schedule.getStartDate() : null,
                recurring ? schedule.getEndDate() : null,
                schedule.getStartTime(),
                schedule.getEndTime(),
                recurring ? schedule.getRrule() : null,
                schedule.getContent(),
                category
        );
    }

    private PartySchedule getSchedule(Long scheduleId) {
        return partyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_SCHEDULE_NOT_FOUND));
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

    private Project getLeaderOwnedProject(Long memberId, Long projectId) {
        Project project = getProject(projectId);
        ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!pm.isLeader()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return project;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }

    private void validateRrule(String rrule) {
        try {
            new Recur(rrule);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_RRULE);
        }
    }

    /** 프로젝트의 그룹 채팅방에 시스템 메시지를 안내한다. 그룹 채팅방이 아직 없으면 조용히 무시한다. */
    private void announceToGroupChat(Long projectId, String content) {
        chatRoomRepository.findByProjectId(projectId)
                .ifPresent(chatRoom -> chatMessageService.sendSystemMessage(chatRoom.getId(), content));
    }

    private void notify(Member member, Project project, String title, String content) {
        if (member == null) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(member.getId()).orElse(null);
        if (setting == null || !setting.isScheduleNoti()) {
            return;
        }
        notificationHistoryRepository.save(NotificationHistory.builder()
                .member(member)
                .title(title)
                .content(content)
                .type(NotificationType.SCHEDULE)
                .referenceType(NotificationReferenceType.PROJECT)
                .referenceId(project.getId())
                .build());
    }
}

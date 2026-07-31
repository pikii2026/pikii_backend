package com.pickii.domain.schedule.service;

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
import com.pickii.domain.schedule.dto.MeetingPollConfirmRequest;
import com.pickii.domain.schedule.dto.MeetingPollConfirmResponse;
import com.pickii.domain.schedule.dto.MeetingPollCreateRequest;
import com.pickii.domain.schedule.dto.MeetingPollCreateResponse;
import com.pickii.domain.schedule.dto.MeetingPollDetailResponse;
import com.pickii.domain.schedule.dto.MeetingPollResponseSubmitResponse;
import com.pickii.domain.schedule.entity.MeetingPoll;
import com.pickii.domain.schedule.entity.MeetingPollAvailability;
import com.pickii.domain.schedule.entity.MeetingPollMember;
import com.pickii.domain.schedule.entity.MeetingPollSlot;
import com.pickii.domain.schedule.entity.MeetingPollStatus;
import com.pickii.domain.schedule.entity.MemberSchedule;
import com.pickii.domain.schedule.entity.PartySchedule;
import com.pickii.domain.schedule.entity.PartyScheduleAttendance;
import com.pickii.domain.schedule.repository.MeetingPollAvailabilityRepository;
import com.pickii.domain.schedule.repository.MeetingPollMemberRepository;
import com.pickii.domain.schedule.repository.MeetingPollRepository;
import com.pickii.domain.schedule.repository.MeetingPollSlotRepository;
import com.pickii.domain.schedule.repository.MemberScheduleRepository;
import com.pickii.domain.schedule.repository.PartyScheduleAttendanceRepository;
import com.pickii.domain.schedule.repository.PartyScheduleRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 회의 조율 개설/응답/확정/취소 (API_SPEC 7-10 ~ 7-14)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingPollService {

    private static final Set<Integer> ALLOWED_DURATION_MINUTES = Set.of(30, 60, 90, 120);

    private final MeetingPollRepository meetingPollRepository;
    private final MeetingPollSlotRepository meetingPollSlotRepository;
    private final MeetingPollMemberRepository meetingPollMemberRepository;
    private final MeetingPollAvailabilityRepository meetingPollAvailabilityRepository;
    private final MemberScheduleRepository memberScheduleRepository;
    private final PartyScheduleRepository partyScheduleRepository;
    private final PartyScheduleAttendanceRepository partyScheduleAttendanceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    /** 7-10 회의 조율 개설 */
    @Transactional
    public MeetingPollCreateResponse open(Long memberId, Long projectId, MeetingPollCreateRequest request) {
        Project project = getLeaderOwnedProject(memberId, projectId);
        if (meetingPollRepository.existsByProjectIdAndStatus(projectId, MeetingPollStatus.COLLECTING)) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_ACTIVE);
        }
        if (request.rangeStart().isAfter(request.rangeEnd())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "탐색 시작일은 종료일보다 이전이어야 합니다.");
        }
        if (!request.dayStart().isBefore(request.dayEnd())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "탐색 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }
        if (!ALLOWED_DURATION_MINUTES.contains(request.durationMin())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "소요 시간은 30/60/90/120분 중 하나여야 합니다.");
        }

        int deadlineHours = request.deadlineHours() != null ? request.deadlineHours() : 12;
        LocalDateTime deadline = LocalDateTime.now().plusHours(deadlineHours);

        MeetingPoll poll = MeetingPoll.builder()
                .project(project)
                .createdBy(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .durationMin(request.durationMin())
                .rangeStart(request.rangeStart())
                .rangeEnd(request.rangeEnd())
                .dayStart(request.dayStart())
                .dayEnd(request.dayEnd())
                .deadline(deadline)
                .build();
        meetingPollRepository.save(poll);

        List<MeetingPollSlot> slots = generateSlots(poll);
        meetingPollSlotRepository.saveAll(slots);

        List<ProjectMember> participants = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(projectId);
        if (request.memberIds() != null) {
            participants = participants.stream()
                    .filter(pm -> request.memberIds().contains(pm.getMember().getId()))
                    .toList();
        }
        participants.forEach(pm -> {
            meetingPollMemberRepository.save(new MeetingPollMember(poll.getId(), pm.getMember().getId()));
            notify(pm.getMember(), project, "회의 조율이 시작되었습니다.", "'" + poll.getTitle() + "' 회의 시간 조율에 응답해주세요.");
        });
        // TODO: 그룹 채팅방 공지 (채팅 시스템 메시지 기능 연동 후 추가)

        return new MeetingPollCreateResponse(
                poll.getId(),
                poll.getStatus().name(),
                deadline.atOffset(ZoneOffset.ofHours(9)),
                participants.size(),
                0,
                slots.size()
        );
    }

    /** 7-11 응답 화면 조회 (슬롯 + 캘린더 프리필) */
    public MeetingPollDetailResponse getDetail(Long memberId, Long pollId) {
        MeetingPoll poll = getPoll(pollId);
        requireActiveProjectMember(poll.getProject().getId(), memberId);

        List<MeetingPollSlot> slots = meetingPollSlotRepository.findAllByPollIdOrderByStartAt(pollId);
        List<Long> slotIds = slots.stream().map(MeetingPollSlot::getId).toList();

        Map<Long, List<MeetingPollAvailability>> availabilitiesBySlot = meetingPollAvailabilityRepository
                .findAllBySlotIdIn(slotIds).stream()
                .collect(Collectors.groupingBy(MeetingPollAvailability::getSlotId));

        List<MemberSchedule> mySchedules = memberScheduleRepository
                .findAllByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        memberId, poll.getRangeEnd(), poll.getRangeStart());
        Map<LocalDate, List<MemberSchedule>> busySchedulesByDate = buildBusyScheduleMap(mySchedules, poll.getRangeStart(), poll.getRangeEnd());

        long totalMembers = meetingPollMemberRepository.countByPollId(pollId);
        long respondedCount = meetingPollMemberRepository.countByPollIdAndRespondedTrue(pollId);
        boolean myResponded = meetingPollMemberRepository.findById(new MeetingPollMember.Pk(pollId, memberId))
                .map(MeetingPollMember::isResponded)
                .orElse(false);

        List<MeetingPollDetailResponse.SlotItem> slotItems = slots.stream()
                .map(slot -> toSlotItem(slot, memberId, totalMembers, availabilitiesBySlot.getOrDefault(slot.getId(), List.of()), busySchedulesByDate))
                .toList();

        return new MeetingPollDetailResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getStatus().name(),
                poll.getDurationMin(),
                poll.getDeadline().atOffset(ZoneOffset.ofHours(9)),
                totalMembers,
                respondedCount,
                myResponded,
                slotItems
        );
    }

    private MeetingPollDetailResponse.SlotItem toSlotItem(MeetingPollSlot slot, Long memberId, long totalMembers,
                                                            List<MeetingPollAvailability> slotAvailabilities,
                                                            Map<LocalDate, List<MemberSchedule>> busySchedulesByDate) {
        MeetingPollAvailability myAvailability = slotAvailabilities.stream()
                .filter(a -> a.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);

        boolean prefilledByCalendar = myAvailability == null && overlapsBusySchedule(slot, busySchedulesByDate);
        boolean myAvailable = myAvailability != null ? myAvailability.isAvailable() : !prefilledByCalendar;
        long availableCount = slotAvailabilities.stream().filter(MeetingPollAvailability::isAvailable).count();
        long unansweredCount = totalMembers - slotAvailabilities.size();

        return new MeetingPollDetailResponse.SlotItem(
                slot.getId(),
                slot.getStartAt().atOffset(ZoneOffset.ofHours(9)),
                slot.getEndAt().atOffset(ZoneOffset.ofHours(9)),
                myAvailable,
                prefilledByCalendar,
                availableCount,
                unansweredCount
        );
    }

    /** 7-12 응답 제출 */
    @Transactional
    public MeetingPollResponseSubmitResponse submit(Long memberId, Long pollId, List<Long> unavailableSlotIds) {
        MeetingPoll poll = getPoll(pollId);
        if (!poll.isCollecting()) {
            throw new BusinessException(ErrorCode.POLL_NOT_COLLECTING);
        }
        MeetingPollMember pollMember = meetingPollMemberRepository.findById(new MeetingPollMember.Pk(pollId, memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        List<MeetingPollSlot> allSlots = meetingPollSlotRepository.findAllByPollIdOrderByStartAt(pollId);
        Set<Long> validSlotIds = allSlots.stream().map(MeetingPollSlot::getId).collect(Collectors.toSet());
        if (!validSlotIds.containsAll(unavailableSlotIds)) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        Set<Long> unavailableSet = new HashSet<>(unavailableSlotIds);

        for (MeetingPollSlot slot : allSlots) {
            boolean available = !unavailableSet.contains(slot.getId());
            MeetingPollAvailability.Pk pk = new MeetingPollAvailability.Pk(slot.getId(), memberId);
            meetingPollAvailabilityRepository.findById(pk)
                    .ifPresentOrElse(
                            existing -> existing.changeAvailable(available),
                            () -> meetingPollAvailabilityRepository.save(new MeetingPollAvailability(slot.getId(), memberId, available))
                    );
        }
        pollMember.markResponded();

        long respondedCount = meetingPollMemberRepository.countByPollIdAndRespondedTrue(pollId);
        long totalMembers = meetingPollMemberRepository.countByPollId(pollId);
        if (respondedCount == totalMembers) {
            notify(poll.getCreatedBy(), poll.getProject(), "회의 조율 응답이 모두 완료되었습니다.",
                    "'" + poll.getTitle() + "' 회의 조율에 전원이 응답하여 확정할 수 있습니다.");
        }
        return new MeetingPollResponseSubmitResponse(pollId, respondedCount, totalMembers);
    }

    /** 7-13 최종 일정 확정 */
    @Transactional
    public MeetingPollConfirmResponse confirm(Long memberId, Long pollId, MeetingPollConfirmRequest request) {
        MeetingPoll poll = getPoll(pollId);
        requireLeader(poll.getProject().getId(), memberId);
        if (!poll.isCollecting()) {
            throw new BusinessException(ErrorCode.POLL_NOT_COLLECTING);
        }
        MeetingPollSlot slot = meetingPollSlotRepository.findById(request.slotId())
                .filter(s -> s.getPoll().getId().equals(pollId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

        boolean force = request.force() != null && request.force();
        long totalMembers = meetingPollMemberRepository.countByPollId(pollId);
        long answeredForSlot = meetingPollAvailabilityRepository.countBySlotId(slot.getId());
        if (!force && answeredForSlot < totalMembers) {
            throw new BusinessException(ErrorCode.UNANSWERED_EXISTS);
        }

        LocalDateTime scheduleStart = slot.getStartAt();
        LocalDateTime scheduleEnd = scheduleStart.plusMinutes(poll.getDurationMin());
        PartySchedule schedule = PartySchedule.builder()
                .project(poll.getProject())
                .startDate(scheduleStart.toLocalDate())
                .endDate(scheduleEnd.toLocalDate())
                .startTime(scheduleStart.toLocalTime())
                .endTime(scheduleEnd.toLocalTime())
                .rrule(null)
                .exDate(null)
                .title(poll.getTitle())
                .content(null)
                .build();
        partyScheduleRepository.save(schedule);
        poll.confirm(schedule);

        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(poll.getProject().getId());
        members.forEach(pm -> {
            partyScheduleAttendanceRepository.save(new PartyScheduleAttendance(schedule.getId(), pm.getMember().getId()));
            notify(pm.getMember(), poll.getProject(), "회의 일정이 확정되었습니다.",
                    "'" + poll.getTitle() + "' 회의가 " + scheduleStart + "로 확정되었습니다.");
        });
        // TODO: 그룹 채팅방 공지 (채팅 시스템 메시지 기능 연동 후 추가)

        return new MeetingPollConfirmResponse(poll.getId(), poll.getStatus().name(), schedule.getId());
    }

    /** 7-14 조율 취소 / 재조율 */
    @Transactional
    public void cancel(Long memberId, Long pollId) {
        MeetingPoll poll = getPoll(pollId);
        requireLeader(poll.getProject().getId(), memberId);

        PartySchedule linkedSchedule = poll.getSchedule();
        poll.cancel();
        if (linkedSchedule != null) {
            partyScheduleAttendanceRepository.deleteByScheduleId(linkedSchedule.getId());
            partyScheduleRepository.delete(linkedSchedule);
        }

        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdAndLeftAtIsNull(poll.getProject().getId());
        members.forEach(pm -> notify(pm.getMember(), poll.getProject(), "회의 조율이 취소되었습니다.",
                "'" + poll.getTitle() + "' 회의 조율이 취소되었습니다."));
    }

    /**
     * 탐색 기간 × 탐색 시간대를 30분 간격 후보 시작 시각으로 나누고, 각 슬롯의 길이는
     * 회의 소요 시간(durationMin)만큼 잡아 후보 슬롯 전체를 생성한다.
     * (예: durationMin=60이면 09:00~10:00, 09:30~10:30, 10:00~11:00 ... 30분씩 밀려가며 겹치는 1시간 슬롯이 나온다.)
     */
    private List<MeetingPollSlot> generateSlots(MeetingPoll poll) {
        List<MeetingPollSlot> slots = new ArrayList<>();
        int durationMin = poll.getDurationMin();
        for (LocalDate date = poll.getRangeStart(); !date.isAfter(poll.getRangeEnd()); date = date.plusDays(1)) {
            LocalDateTime cursor = LocalDateTime.of(date, poll.getDayStart());
            LocalDateTime dayEnd = LocalDateTime.of(date, poll.getDayEnd());
            while (!cursor.plusMinutes(durationMin).isAfter(dayEnd)) {
                slots.add(new MeetingPollSlot(poll, cursor, cursor.plusMinutes(durationMin)));
                cursor = cursor.plusMinutes(30);
            }
        }
        return slots;
    }

    /** 개인 일정(단발/반복)을 조율 기간 내에서 날짜별로 전개하여 맵으로 구성한다. */
    private Map<LocalDate, List<MemberSchedule>> buildBusyScheduleMap(List<MemberSchedule> schedules, LocalDate rangeStart, LocalDate rangeEnd) {
        Map<LocalDate, List<MemberSchedule>> map = new HashMap<>();
        for (MemberSchedule schedule : schedules) {
            LocalDate expandStart = schedule.getStartDate().isAfter(rangeStart) ? schedule.getStartDate() : rangeStart;
            LocalDate expandEnd = schedule.getEndDate().isBefore(rangeEnd) ? schedule.getEndDate() : rangeEnd;
            if (expandStart.isAfter(expandEnd)) {
                continue;
            }
            for (LocalDate date : resolveOccurrenceDates(schedule, expandStart, expandEnd)) {
                map.computeIfAbsent(date, d -> new ArrayList<>()).add(schedule);
            }
        }
        return map;
    }

    private List<LocalDate> resolveOccurrenceDates(MemberSchedule schedule, LocalDate expandStart, LocalDate expandEnd) {
        if (!schedule.isRecurring()) {
            List<LocalDate> dates = new ArrayList<>();
            for (LocalDate date = expandStart; !date.isAfter(expandEnd); date = date.plusDays(1)) {
                dates.add(date);
            }
            return dates;
        }
        try {
            Recur recur = new Recur(schedule.getRrule());
            Date seed = toIcalDate(schedule.getStartDate());
            Date periodStart = toIcalDate(expandStart);
            Date periodEnd = toIcalDate(expandEnd);
            DateList occurrences = recur.getDates(seed, periodStart, periodEnd, Value.DATE);
            Set<LocalDate> excluded = parseExDates(schedule.getExDate());
            List<LocalDate> dates = new ArrayList<>();
            for (Date occurrence : occurrences) {
                LocalDate date = occurrence.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                if (!excluded.contains(date)) {
                    dates.add(date);
                }
            }
            return dates;
        } catch (Exception e) {
            // RRULE이 유효하지 않으면(생성/수정 시 이미 검증되었어야 하지만) 프리필을 생략한다.
            return List.of();
        }
    }

    /**
     * ical4j의 Date는 내부적으로 UTC 자정 기준 epoch millis로 날짜를 다룬다.
     * {@code java.sql.Date.valueOf}(로컬 자정 기준)로 변환하면 KST(UTC+9)에서는
     * UTC 기준 하루 전 날짜로 잘못 해석되므로, UTC 자정 millis로 직접 변환한다.
     */
    private Date toIcalDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    private Set<LocalDate> parseExDates(String exDate) {
        if (exDate == null || exDate.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(exDate.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
    }

    private boolean overlapsBusySchedule(MeetingPollSlot slot, Map<LocalDate, List<MemberSchedule>> busySchedulesByDate) {
        LocalDate slotDate = slot.getStartAt().toLocalDate();
        LocalTime slotStart = slot.getStartAt().toLocalTime();
        LocalTime slotEnd = slot.getEndAt().toLocalTime();
        return busySchedulesByDate.getOrDefault(slotDate, List.of()).stream()
                .anyMatch(schedule -> slotStart.isBefore(schedule.getEndTime()) && schedule.getStartTime().isBefore(slotEnd));
    }

    private MeetingPoll getPoll(Long pollId) {
        return meetingPollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));
    }

    private void requireActiveProjectMember(Long projectId, Long memberId) {
        if (!projectMemberRepository.existsByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireLeader(Long projectId, Long memberId) {
        ProjectMember pm = projectMemberRepository.findByProjectIdAndMemberIdAndLeftAtIsNull(projectId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!pm.isLeader()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Project getLeaderOwnedProject(Long memberId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        requireLeader(projectId, memberId);
        return project;
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
                .type(NotificationType.MEETING)
                .referenceType(NotificationReferenceType.PROJECT)
                .referenceId(project.getId())
                .build());
    }
}

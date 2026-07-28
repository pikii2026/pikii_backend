package com.pickii.domain.schedule.service;

import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.dto.RecurringScheduleRequest;
import com.pickii.domain.schedule.dto.ScheduleCategoryResponse;
import com.pickii.domain.schedule.dto.ScheduleCreateResponse;
import com.pickii.domain.schedule.dto.ScheduleUpdateRequest;
import com.pickii.domain.schedule.dto.SingleScheduleRequest;
import com.pickii.domain.schedule.entity.MemberSchedule;
import com.pickii.domain.schedule.entity.ScheduleCategory;
import com.pickii.domain.schedule.repository.MemberScheduleRepository;
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
 * 월별 일정 조회 (API_SPEC 7-5), 개인 단발/반복 일정 생성 (7-6, 7-7),
 * 개인 일정 수정 (7-8), 삭제 (7-9)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberScheduleService {

    private final MemberScheduleRepository memberScheduleRepository;
    private final ScheduleCategoryRepository scheduleCategoryRepository;
    private final MemberRepository memberRepository;

    /** 7-5 월별 일정 조회. 반복 일정은 RRULE을 그대로 반환하고, 실제 날짜 전개는 클라이언트가 수행한다. */
    public List<MyScheduleResponse> getMonthlySchedules(Long memberId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate rangeStart = yearMonth.atDay(1);
        LocalDate rangeEnd = yearMonth.atEndOfMonth();

        return memberScheduleRepository
                .findAllByMemberIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(memberId, rangeEnd, rangeStart)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 7-6 개인 단발 일정 생성 */
    @Transactional
    public ScheduleCreateResponse createSingle(Long memberId, SingleScheduleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        ScheduleCategory category = resolveCategory(memberId, request.categoryId());

        MemberSchedule schedule = MemberSchedule.builder()
                .member(memberRepository.getReferenceById(memberId))
                .startDate(request.date())
                .endDate(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .rrule(null)
                .title(request.title())
                .content(request.content())
                .category(category)
                .build();
        memberScheduleRepository.save(schedule);
        return new ScheduleCreateResponse(schedule.getId());
    }

    /** 7-7 개인 반복 일정 생성 */
    @Transactional
    public ScheduleCreateResponse createRecurring(Long memberId, RecurringScheduleRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 시작일은 종료일보다 이전이어야 합니다.");
        }
        validateTimeRange(request.startTime(), request.endTime());
        validateRrule(request.rrule());
        ScheduleCategory category = resolveCategory(memberId, request.categoryId());

        MemberSchedule schedule = MemberSchedule.builder()
                .member(memberRepository.getReferenceById(memberId))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .rrule(request.rrule())
                .title(request.title())
                .content(request.content())
                .category(category)
                .build();
        memberScheduleRepository.save(schedule);
        return new ScheduleCreateResponse(schedule.getId());
    }

    /** 7-8 개인 일정 수정. 단발↔반복 전환은 지원하지 않으며, 기존 일정의 종류에 맞는 필드만 사용한다. */
    @Transactional
    public void update(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
        MemberSchedule schedule = getOwnedSchedule(memberId, scheduleId);
        validateTimeRange(request.startTime(), request.endTime());
        ScheduleCategory category = resolveCategory(memberId, request.categoryId());

        if (schedule.isRecurring()) {
            if (request.startDate() == null || request.endDate() == null || request.rrule() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 일정은 startDate/endDate/rrule이 필요합니다.");
            }
            if (request.startDate().isAfter(request.endDate())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "반복 시작일은 종료일보다 이전이어야 합니다.");
            }
            validateRrule(request.rrule());
            schedule.update(request.title(), request.startDate(), request.endDate(),
                    request.startTime(), request.endTime(), request.rrule(), request.content(), category);
        } else {
            if (request.date() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "단발 일정은 date가 필요합니다.");
            }
            schedule.update(request.title(), request.date(), request.date(),
                    request.startTime(), request.endTime(), null, request.content(), category);
        }
    }

    /** 7-9 개인 일정 삭제 (반복 일정은 전체 삭제) */
    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        MemberSchedule schedule = getOwnedSchedule(memberId, scheduleId);
        memberScheduleRepository.delete(schedule);
    }

    private MemberSchedule getOwnedSchedule(Long memberId, Long scheduleId) {
        MemberSchedule schedule = memberScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return schedule;
    }

    private ScheduleCategory resolveCategory(Long memberId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return scheduleCategoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_CATEGORY_NOT_FOUND));
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

    private MyScheduleResponse toResponse(MemberSchedule schedule) {
        boolean recurring = schedule.isRecurring();
        ScheduleCategoryResponse category = schedule.getCategory() == null
                ? null
                : ScheduleCategoryResponse.from(schedule.getCategory());

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
}

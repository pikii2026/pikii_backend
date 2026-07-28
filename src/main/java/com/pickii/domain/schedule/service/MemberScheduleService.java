package com.pickii.domain.schedule.service;

import com.pickii.domain.schedule.dto.MyScheduleResponse;
import com.pickii.domain.schedule.dto.ScheduleCategoryResponse;
import com.pickii.domain.schedule.entity.MemberSchedule;
import com.pickii.domain.schedule.repository.MemberScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 월별 일정 조회 (API_SPEC 7-5)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberScheduleService {

    private final MemberScheduleRepository memberScheduleRepository;

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

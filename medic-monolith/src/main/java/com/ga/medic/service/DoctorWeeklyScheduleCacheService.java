package com.ga.medic.service;

import com.ga.medic.model.AvailabilityException;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorScheduledSlot;
import com.ga.medic.repository.AvailabilityExceptionRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates and caches schedule-only slots for one doctor and Monday-Sunday calendar week.
 */
@Service
@RequiredArgsConstructor
public class DoctorWeeklyScheduleCacheService {

    private static final String CACHE_NAME = "doctor-weekly-schedule";

    private final AvailabilityRuleRepository ruleRepository;
    private final AvailabilityExceptionRepository exceptionRepository;

    /**
     * Returns the doctor's scheduled slots for the specified week.
     * Past slots are retained, and the result is cached by doctor, week, and schedule version.
     *
     * @param doctorId  the ID of the doctor's profile
     * @param weekStart the Monday marking the beginning of the requested calendar week
     * @return the scheduled slots for the specified doctor and week, including past slots
     */
    @Cacheable(cacheNames = CACHE_NAME,
            key = "'doctor:' + #doctorId + ':week:' + #weekStart + ':version:' + "
                    + "@doctorScheduleCacheVersionService.getVersion(#doctorId)")
    public List<DoctorScheduledSlot> getWeekSchedule(Long doctorId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<AvailabilityRule> rules = ruleRepository.findByDoctorIdAndDateRange(doctorId, weekStart, weekEnd);
        List<AvailabilityException> exceptions = exceptionRepository
                .findByDoctorAndDateRange(doctorId, weekStart, weekEnd);

        Set<ScheduleSlotKey> seenSlots = new HashSet<>();
        List<DoctorScheduledSlot> slots = new ArrayList<>();

        for (AvailabilityRule rule : rules) {
            LocalDate ruleStartDate = rule.getStartDate().isAfter(weekStart) ? rule.getStartDate() : weekStart;
            LocalDate ruleEndDate = rule.getEndDate() == null || rule.getEndDate().isAfter(weekEnd)
                    ? weekEnd : rule.getEndDate();
            if (ruleStartDate.isAfter(ruleEndDate)) {
                continue;
            }

            for (LocalDate date = ruleStartDate; !date.isAfter(ruleEndDate); date = date.plusDays(1)) {
                if (!rule.getDaysOfWeek().contains(date.getDayOfWeek())) {
                    continue;
                }

                int slotMinutes = rule.getSlotMinutes();
                LocalTime slotStart = rule.getStartTime();

                while (!slotStart.plusMinutes(slotMinutes).isAfter(rule.getEndTime())) {
                    LocalTime slotEnd = slotStart.plusMinutes(slotMinutes);
                    LocalDate slotDate = date;
                    LocalTime exactStart = slotStart;

                    boolean inException = exceptions.stream().anyMatch(exception -> {
                        if (!exception.getExceptionDate().equals(slotDate)) {
                            return false;
                        }
                        if (exception.getStartTime() == null && exception.getEndTime() == null) {
                            return true;
                        }
                        return exactStart.isBefore(exception.getEndTime())
                                && slotEnd.isAfter(exception.getStartTime());
                    });

                    if (!inException && seenSlots.add(new ScheduleSlotKey(slotDate, exactStart))) {
                        slots.add(new DoctorScheduledSlot(rule.getId(), slotDate, exactStart, slotEnd, slotMinutes));
                    }

                    slotStart = slotEnd;
                }
            }
        }

        return List.copyOf(slots);
    }

    private record ScheduleSlotKey(LocalDate date, LocalTime startTime) {
    }
}

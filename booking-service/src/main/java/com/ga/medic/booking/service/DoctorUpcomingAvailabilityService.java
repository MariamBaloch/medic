package com.ga.medic.booking.service;

import com.ga.medic.booking.dto.response.DoctorUpcomingAvailabilityResponse;
import com.ga.medic.booking.repository.AvailabilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorUpcomingAvailabilityService {

    private final AvailabilityRuleRepository availabilityRuleRepository;

    @Transactional(readOnly = true)
    public List<DoctorUpcomingAvailabilityResponse> getUpcomingAvailability(List<Long> doctorProfileIds) {
        if (doctorProfileIds == null || doctorProfileIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return doctorProfileIds.stream()
                .distinct()
                .map(id -> new DoctorUpcomingAvailabilityResponse(
                        id,
                        !availabilityRuleRepository.findCurrentAndFuture(id, today).isEmpty()
                ))
                .toList();
    }
}

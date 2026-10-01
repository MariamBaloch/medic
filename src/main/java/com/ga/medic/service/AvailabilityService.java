package com.ga.medic.service;

import com.ga.medic.dto.request.AvailabilityRuleRequest;
import com.ga.medic.dto.response.RuleUpdateResponse;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.mapper.AvailabilityRuleMapper;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRuleRepository ruleRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AvailabilityRuleMapper ruleMapper;

    @Transactional
    public RuleUpdateResponse createRule(AvailabilityRuleRequest request) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        checkRuleOverlap(doctor.getId(), request, -1L);

        AvailabilityRule rule = ruleMapper.toEntity(request);
        rule.setDoctor(doctor);
        rule = ruleRepository.save(rule);
        return new RuleUpdateResponse(ruleMapper.toResponse(rule), null, null);
    }

    /**
     * Check that a new/updated rule does not overlap with existing rules for the same doctor.
     * Overlap = same date range, shared weekday, overlapping time window.
     */
    private void checkRuleOverlap(Long doctorId, AvailabilityRuleRequest request, Long excludeId) {
        LocalDate endDate = request.endDate() != null ? request.endDate() : LocalDate.of(9999, 12, 31);
        List<AvailabilityRule> existing = ruleRepository.findOverlappingExcluding(doctorId, request.startDate(), endDate, excludeId != null ? excludeId : -1L);

        for (AvailabilityRule rule : existing) {
            Set<DayOfWeek> sharedDays = new HashSet<>(rule.getDaysOfWeek());
            sharedDays.retainAll(request.daysOfWeek());
            if (sharedDays.isEmpty()) continue;

            if (request.startTime().isBefore(rule.getEndTime()) && request.endTime().isAfter(rule.getStartTime())) {
                throw new InformationExistsException("This rule overlaps with an existing rule (ID " + rule.getId() + ") on " + sharedDays + " between " + rule.getStartTime() + " and " + rule.getEndTime());
            }
        }
    }
}
package com.ga.medic.service;

import com.ga.medic.dto.request.AvailabilityRuleRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AvailabilityRuleDeleteResponse;
import com.ga.medic.dto.response.AvailabilityRuleResponse;
import com.ga.medic.dto.response.AvailabilityRuleUpdateResponse;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.AvailabilityRuleMapper;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AvailabilityRuleService {

    private final AvailabilityRuleRepository ruleRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AvailabilityRuleMapper ruleMapper;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    /**
     * Returns all current and future availability rules for the logged-in doctor.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> listRules() {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        List<AvailabilityRule> rules = ruleRepository.findCurrentAndFuture(doctor.getId(), LocalDate.now());
        return ruleMapper.toResponseList(rules);
    }

    /**
     * Creates a new availability rule.
     * Checks for overlap with any existing rule for the same doctor.
     */
    @Transactional
    public AvailabilityRuleUpdateResponse createRule(AvailabilityRuleRequest request) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        checkRuleOverlap(doctor.getId(), request, -1L);

        AvailabilityRule rule = ruleMapper.toEntity(request);
        rule.setDoctor(doctor);
        rule = ruleRepository.save(rule);
        return new AvailabilityRuleUpdateResponse(ruleMapper.toResponse(rule), null, null);
    }

    /*
     * Deletes an availability rule.
     * Hard deletes the rule if it hasn't started and detaches any appointments associated with it.
     * Soft deletes the rule if it has started by setting the end date to yesterday.
     */
    @Transactional
    public AvailabilityRuleDeleteResponse deleteRule(Long ruleId) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();

        AvailabilityRule rule = ruleRepository.findByIdAndDoctorId(ruleId, doctor.getId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule not found"));

        LocalDate today = LocalDate.now();
        List<Appointment> affectedAppointments = new ArrayList<>();
        boolean hardDeleted;
        String message = null;

        if (rule.getStartDate().isAfter(today)) {
            affectedAppointments = appointmentRepository.findByAvailabilityRuleId(ruleId);

            for (Appointment appointment : affectedAppointments) {
                appointment.setAvailabilityRule(null);
            }

            appointmentRepository.saveAll(affectedAppointments);
            ruleRepository.delete(rule);

            hardDeleted = true;
            if (!affectedAppointments.isEmpty()) {
                message = affectedAppointments.size() + " existing appointment(s) were detached from the deleted rule";
            }
        } else {
            rule.setEndDate(today.minusDays(1));
            ruleRepository.save(rule);
            hardDeleted = false;
        }

        List<AppointmentResponse> affectedAppointmentResponses = appointmentMapper.toResponseList(affectedAppointments);
        return new AvailabilityRuleDeleteResponse(true, affectedAppointmentResponses, message);
    }

    /**
     * Updates an existing availability rule.
     * If the existing rule has not started yet, it is updated directly.
     * If the rule is already active, the old rule is ended and a new rule
     * is created from the requested start date so that past schedules are preserved.
     */
    @Transactional
    public AvailabilityRuleUpdateResponse updateRule(Long ruleId, AvailabilityRuleRequest request) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        AvailabilityRule existing = ruleRepository.findByIdAndDoctorId(ruleId, doctor.getId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule not found"));

        LocalDate today = LocalDate.now();

        if (request.startDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        List<Appointment> affectedAppointments;

        if (existing.getStartDate().isAfter(today)) {
            checkRuleOverlap(doctor.getId(), request, existing.getId());
            ruleMapper.updateRuleFromRequest(request, existing);
            existing = ruleRepository.save(existing);

            affectedAppointments = findAppointmentsOutsideRule(doctor.getId(), existing);
        } else {
            LocalDate newStartDate = request.startDate();

            existing.setEndDate(newStartDate.minusDays(1));
            ruleRepository.save(existing);

            checkRuleOverlap(doctor.getId(), request, existing.getId());

            AvailabilityRule newRule = ruleMapper.toEntity(request);
            newRule.setDoctor(doctor);

            newRule = ruleRepository.save(newRule);
            existing = newRule;

            affectedAppointments = findAppointmentsOutsideRule(doctor.getId(), newRule);
        }
        List<AppointmentResponse> affectedAppointmentResponseList = affectedAppointments.isEmpty() ? null : appointmentMapper.toResponseList(affectedAppointments);
        String message = (affectedAppointmentResponseList != null) ? affectedAppointmentResponseList.size() + " existing appointment(s) outside the new schedule were kept" : null;
        return new AvailabilityRuleUpdateResponse(ruleMapper.toResponse(existing), affectedAppointmentResponseList, message);
    }

    /**
     * Finds future active appointments that fall outside the schedule defined by the given availability rule.
     * Appointments are not deleted or canceled; they are returned to inform the doctor that they are outside the new schedule.
     */
    private List<Appointment> findAppointmentsOutsideRule(Long doctorId, AvailabilityRule rule) {
        LocalDate from = rule.getStartDate().isBefore(LocalDate.now()) ? LocalDate.now() : rule.getStartDate();
        LocalDate to = rule.getEndDate() != null ? rule.getEndDate() : from.plusYears(1);

        List<Appointment> futureAppointments = appointmentRepository.findActiveByDoctorAndDateRange(doctorId, from, to);

        List<Appointment> outsideAppointments = new ArrayList<>();
        for (Appointment appointment : futureAppointments) {
            boolean outsideSchedule = false;

            if (!rule.getDaysOfWeek().contains(appointment.getAppointmentDate().getDayOfWeek())) {
                outsideSchedule = true;
            } else if (appointment.getStartTime().isBefore(rule.getStartTime()) || appointment.getEndTime().isAfter(rule.getEndTime())) {
                outsideSchedule = true;
            }

            if (outsideSchedule) {
                outsideAppointments.add(appointment);
            }
        }
        return outsideAppointments;
    }

    /**
     * Checks whether a new or updated rule overlaps with another rule belonging to the same doctor.
     * Two rules overlap when:
     * - Their date ranges overlap.
     * - They share at least one day of the week.
     * - Their time ranges overlap.
     * excludeId is used when updating a rule so that the rule being updated is not compared against itself.
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
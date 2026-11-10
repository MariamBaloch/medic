package com.ga.medic.service;

import com.ga.medic.common.security.AuthenticatedUser;
import com.ga.medic.dto.request.AvailabilityExceptionRequest;
import com.ga.medic.dto.response.AvailabilityExceptionDeleteResponse;
import com.ga.medic.dto.response.AvailabilityExceptionResponse;
import com.ga.medic.event.DoctorScheduleChangedEvent;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.AvailabilityExceptionMapper;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityException;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityExceptionService {

    private final AvailabilityExceptionRepository exceptionRepository;
    private final AvailabilityExceptionMapper exceptionMapper;
    private final AppointmentRepository appointmentRepository;
    private final AuthenticatedUser authenticatedUser;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves current and future availability exceptions for the authenticated doctor.
     *
     * @return the doctor's current and future availability exceptions
     */
    @Transactional
    public List<AvailabilityExceptionResponse> listExceptions() {
        Long doctorId = authenticatedUser.requireDoctorId();
        List<AvailabilityException> exceptions = exceptionRepository.findCurrentAndFutureByDoctorId(doctorId, LocalDate.now());
        return exceptionMapper.toResponseList(exceptions);
    }

    /**
     * Creates an availability exception after checking for overlaps and identifying affected appointments.
     *
     * @param request the date and optional time window to make unavailable
     * @return the saved exception and any appointments it affects
     */
    @Transactional
    public AvailabilityExceptionResponse createException(AvailabilityExceptionRequest request) {
        Long doctorId = authenticatedUser.requireDoctorId();
        checkExceptionOverlap(doctorId, request, -1L);

        AvailabilityException exception = exceptionMapper.toEntity(request);
        exception.setDoctorId(doctorId);
        return getAvailabilityExceptionResponse(request, doctorId, exception);
    }

    /**
     * Updates an existing availability exception for the authenticated doctor.
     *
     * @param exceptionId the ID of the exception to update
     * @param request     the replacement date and optional time window
     * @return the updated exception and any appointments it affects
     */
    @Transactional
    public AvailabilityExceptionResponse updateException(Long exceptionId, AvailabilityExceptionRequest request) {
        Long doctorId = authenticatedUser.requireDoctorId();

        AvailabilityException existing = exceptionRepository.findByIdAndDoctorId(exceptionId, doctorId)
                .orElseThrow(() -> new InformationNotFoundException("Availability exception not found"));

        checkExceptionOverlap(doctorId, request, existing.getId());

        exceptionMapper.updateExceptionFromRequest(request, existing);
        return getAvailabilityExceptionResponse(request, doctorId, existing);
    }

    /**
     * Saves an exception and builds a response that includes appointments affected by it.
     *
     * @param request   the date and optional time window of the exception
     * @param doctorId  the doctor's profile ID who owns the exception
     * @param existing  the new or updated exception to save
     * @return the saved exception response and affected appointment details
     */
    private AvailabilityExceptionResponse getAvailabilityExceptionResponse(
            AvailabilityExceptionRequest request,
            Long doctorId,
            AvailabilityException existing
    ) {
        existing = exceptionRepository.save(existing);
        publishScheduleChanged(doctorId);

        List<Appointment> affectedAppointments;
        if (request.startTime() == null) {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDate(doctorId, request.exceptionDate());
        } else {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDateAndTimeRange(
                    doctorId, request.exceptionDate(), request.startTime(), request.endTime());
        }

        String message = affectedAppointments.isEmpty()
                ? null
                : affectedAppointments.size() + " existing appointment(s) fall within this exception";
        return exceptionMapper.toResponse(existing, affectedAppointments, message);
    }

    /**
     * Deletes an exception and reports appointments that remain affected by its removal.
     *
     * @param exceptionId the ID of the exception to delete
     * @return the deletion response, including appointments affected by removing the exception
     */
    @Transactional
    public AvailabilityExceptionDeleteResponse deleteException(Long exceptionId) {
        Long doctorId = authenticatedUser.requireDoctorId();

        AvailabilityException exception = exceptionRepository.findByIdAndDoctorId(exceptionId, doctorId)
                .orElseThrow(() -> new InformationNotFoundException("Availability exception not found"));

        List<Appointment> affectedAppointments = appointmentRepository.findActiveByDoctorAndDate(
                doctorId, exception.getExceptionDate());

        exceptionRepository.delete(exception);
        publishScheduleChanged(doctorId);

        String message = affectedAppointments.isEmpty()
                ? null
                : affectedAppointments.size() + " existing appointment(s) are affected by the deleted exception but were kept";

        return exceptionMapper.toDeleteResponse(affectedAppointments, message);
    }

    /**
     * Rejects an exception that overlaps another exception for the same doctor and date.
     *
     * @param doctorId  the doctor's profile ID
     * @param request   the exception window to check
     * @param excludeId the existing exception ID to ignore, or null when creating an exception
     */
    private void checkExceptionOverlap(Long doctorId, AvailabilityExceptionRequest request, Long excludeId) {
        List<AvailabilityException> existing = exceptionRepository.findByDoctorAndDateExcluding(
                doctorId, request.exceptionDate(), excludeId != null ? excludeId : -1L);

        for (AvailabilityException exception : existing) {
            if (request.startTime() == null || exception.getStartTime() == null) {
                throw new InformationExistsException("This exception overlaps with an existing exception (ID "
                        + exception.getId() + ") on " + exception.getExceptionDate());
            }

            if (request.startTime().isBefore(exception.getEndTime()) && request.endTime().isAfter(exception.getStartTime())) {
                throw new InformationExistsException("This exception overlaps with an existing exception (ID "
                        + exception.getId() + ") between " + exception.getStartTime() + " and " + exception.getEndTime());
            }
        }
    }

    /**
     * Publishes an in-process schedule-change event so the doctor's week-slot cache is invalidated.
     *
     * @param doctorId the doctor's profile ID whose schedule changed
     */
    private void publishScheduleChanged(Long doctorId) {
        eventPublisher.publishEvent(new DoctorScheduleChangedEvent(doctorId));
    }
}

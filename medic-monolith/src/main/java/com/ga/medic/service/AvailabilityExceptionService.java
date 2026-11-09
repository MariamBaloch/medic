package com.ga.medic.service;

import com.ga.medic.common.event.EventTypes;
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
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityExceptionRepository;
import com.ga.medic.repository.DoctorProfileRepository;
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
    private final DoctorProfileRepository doctorProfileRepository;
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
        DoctorProfile doctor = requireDoctorProfile();

        checkExceptionOverlap(doctor.getId(), request, -1L);

        AvailabilityException exception = exceptionMapper.toEntity(request);
        exception.setDoctorId(doctor.getId());
        return getAvailabilityExceptionResponse(request, doctor, exception);
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
        DoctorProfile doctor = requireDoctorProfile();

        AvailabilityException existing = exceptionRepository.findByIdAndDoctorId(exceptionId, doctor.getId()).orElseThrow(() -> new InformationNotFoundException("Availability exception not found"));

        checkExceptionOverlap(doctor.getId(), request, existing.getId());

        exceptionMapper.updateExceptionFromRequest(request, existing);
        return getAvailabilityExceptionResponse(request, doctor, existing);
    }

    /**
     * Saves an exception and builds a response that includes appointments affected by it.
     *
     * @param request  the date and optional time window of the exception
     * @param doctor   the doctor who owns the exception
     * @param existing the new or updated exception to save
     * @return the saved exception response and affected appointment details
     */
    private AvailabilityExceptionResponse getAvailabilityExceptionResponse(AvailabilityExceptionRequest request, DoctorProfile doctor, AvailabilityException existing) {
        existing = exceptionRepository.save(existing);
        eventPublisher.publishEvent(new DoctorScheduleChangedEvent(doctor.getId()));

        List<Appointment> affectedAppointments;
        if (request.startTime() == null) {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDate(doctor.getId(), request.exceptionDate());
        } else {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDateAndTimeRange(doctor.getId(), request.exceptionDate(), request.startTime(), request.endTime());
        }

        String message = affectedAppointments.isEmpty() ? null : affectedAppointments.size() + " existing appointment(s) are affected by this exception but were kept";

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

        List<Appointment> affectedAppointments;
        if (exception.getStartTime() == null) {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDate(doctorId, exception.getExceptionDate());
        } else {
            affectedAppointments = appointmentRepository.findActiveByDoctorAndDateAndTimeRange(doctorId, exception.getExceptionDate(), exception.getStartTime(), exception.getEndTime());
        }

        exceptionRepository.delete(exception);
        eventPublisher.publishEvent(new DoctorScheduleChangedEvent(doctor.getId()));

        String message = affectedAppointments.isEmpty() ? null : affectedAppointments.size() + " existing appointment(s) are affected by the deleted exception but were kept";

        return exceptionMapper.toDeleteResponse(affectedAppointments, message);
    }

    private DoctorProfile requireDoctorProfile() {
        return doctorProfileRepository.findById(authenticatedUser.requireDoctorId())
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));
    }

    /**
     * Rejects an exception that overlaps another exception for the same doctor and date.
     *
     * @param doctorId  the doctor's profile ID
     * @param request   the exception window to check
     * @param excludeId the existing exception ID to ignore, or null when creating an exception
     */
    private void checkExceptionOverlap(Long doctorId, AvailabilityExceptionRequest request, Long excludeId) {
        List<AvailabilityException> existing = exceptionRepository.findByDoctorAndDateExcluding(doctorId, request.exceptionDate(), excludeId != null ? excludeId : -1L);

        for (AvailabilityException exception : existing) {
            if (request.startTime() == null || exception.getStartTime() == null) {
                throw new InformationExistsException("This exception overlaps with an existing exception (ID " + exception.getId() + ") on " + exception.getExceptionDate());
            }

            if (request.startTime().isBefore(exception.getEndTime()) && request.endTime().isAfter(exception.getStartTime())) {
                throw new InformationExistsException("This exception overlaps with an existing exception (ID " + exception.getId() + ") between " + exception.getStartTime() + " and " + exception.getEndTime());
            }
        }
    }
}

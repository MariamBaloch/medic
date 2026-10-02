package com.ga.medic.service;

import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import com.ga.medic.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AvailabilityRuleService availabilityRuleService;
    private final AvailabilityExceptionService availabilityExceptionService;
    private final AppointmentMapper appointmentMapper;
    private final AuthenticatedUser authenticatedUser;
    private final PageMapper pageMapper;

    @Transactional
    public PageResponse<AppointmentResponse> getMyAppointments(Pageable pageable) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();
        Page<Appointment> appointments = appointmentRepository.findByPatientId(patient.getId(), pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }
}
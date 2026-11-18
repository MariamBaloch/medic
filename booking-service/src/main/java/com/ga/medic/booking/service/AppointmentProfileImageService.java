package com.ga.medic.booking.service;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.booking.dto.response.AppointmentDoctorResponse;
import com.ga.medic.booking.dto.response.AppointmentPatientResponse;
import com.ga.medic.booking.dto.response.AppointmentResponse;
import com.ga.medic.common.contract.InternalProfileImageResponse;
import com.ga.medic.common.contract.InternalProfileImagesResponse;
import com.ga.medic.web.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Appointment rows store names, not photos. Lists still return the current profile photo
 * so the appointment pages can show the same picture as the profile.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentProfileImageService {

    private static final int ID_BATCH = 40;

    private final InternalIdentityClient internalIdentityClient;

    private static List<Long> slice(List<Long> ids, int from) {
        if (from >= ids.size()) {
            return List.of();
        }
        return ids.subList(from, Math.min(ids.size(), from + ID_BATCH));
    }

    private static void putImages(Map<Long, String> target, List<InternalProfileImageResponse> images) {
        if (images == null) {
            return;
        }
        for (InternalProfileImageResponse image : images) {
            if (image == null || image.profileId() == null || image.imageUrl() == null || image.imageUrl().isBlank()) {
                continue;
            }
            target.put(image.profileId(), image.imageUrl());
        }
    }

    private static Set<Long> doctorIds(List<AppointmentResponse> appointments) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AppointmentResponse appointment : appointments) {
            if (appointment.doctor() != null && appointment.doctor().doctorId() != null
                    && isBlank(appointment.doctor().doctorImageUrl())) {
                ids.add(appointment.doctor().doctorId());
            }
        }
        return ids;
    }

    private static Set<Long> patientIds(List<AppointmentResponse> appointments) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AppointmentResponse appointment : appointments) {
            if (appointment.patient() != null && appointment.patient().patientId() != null
                    && isBlank(appointment.patient().patientImageUrl())) {
                ids.add(appointment.patient().patientId());
            }
        }
        return ids;
    }

    private static AppointmentResponse withImages(
            AppointmentResponse appointment,
            Map<Long, String> doctorImages,
            Map<Long, String> patientImages
    ) {
        AppointmentDoctorResponse doctor = appointment.doctor();
        AppointmentPatientResponse patient = appointment.patient();
        String doctorImage = resolve(doctor == null ? null : doctor.doctorImageUrl(),
                doctor == null ? null : doctorImages.get(doctor.doctorId()));
        String patientImage = resolve(patient == null ? null : patient.patientImageUrl(),
                patient == null ? null : patientImages.get(patient.patientId()));
        if (Objects.equals(doctor == null ? null : doctor.doctorImageUrl(), doctorImage)
                && Objects.equals(patient == null ? null : patient.patientImageUrl(), patientImage)) {
            return appointment;
        }
        return new AppointmentResponse(
                appointment.id(),
                appointment.version(),
                doctor == null ? null : new AppointmentDoctorResponse(
                        doctor.doctorId(),
                        doctor.doctorName(),
                        doctorImage,
                        doctor.specialization(),
                        doctor.doctorQualification()
                ),
                patient == null ? null : new AppointmentPatientResponse(
                        patient.patientId(),
                        patient.patientName(),
                        patientImage
                ),
                appointment.appointmentDate(),
                appointment.startTime(),
                appointment.endTime(),
                appointment.bookingFee(),
                appointment.status(),
                appointment.reason(),
                appointment.cancelledReason(),
                appointment.audit()
        );
    }

    private static String resolve(String current, String loaded) {
        if (!isBlank(current)) {
            return current;
        }
        return isBlank(loaded) ? current : loaded;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public PageResponse<AppointmentResponse> apply(PageResponse<AppointmentResponse> page) {
        if (page == null) {
            return null;
        }
        return new PageResponse<>(
                apply(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.first(),
                page.last()
        );
    }

    public List<AppointmentResponse> apply(List<AppointmentResponse> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return appointments == null ? List.of() : appointments;
        }
        try {
            ImageMaps images = loadImages(doctorIds(appointments), patientIds(appointments));
            return appointments.stream()
                    .map(appointment -> withImages(appointment, images.doctors(), images.patients()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Appointment photos were left blank because identity could not be reached: {}", ex.getMessage());
            return appointments;
        }
    }

    private ImageMaps loadImages(Set<Long> doctorIds, Set<Long> patientIds) {
        Map<Long, String> doctors = new HashMap<>();
        Map<Long, String> patients = new HashMap<>();
        List<Long> doctorList = List.copyOf(doctorIds);
        List<Long> patientList = List.copyOf(patientIds);
        if (doctorList.isEmpty() && patientList.isEmpty()) {
            return new ImageMaps(doctors, patients);
        }
        int index = 0;
        while (index < doctorList.size() || index < patientList.size()) {
            InternalProfileImagesResponse batch = internalIdentityClient.getProfileImages(
                    slice(doctorList, index),
                    slice(patientList, index)
            );
            putImages(doctors, batch == null ? null : batch.doctors());
            putImages(patients, batch == null ? null : batch.patients());
            index += ID_BATCH;
        }
        return new ImageMaps(doctors, patients);
    }

    private record ImageMaps(Map<Long, String> doctors, Map<Long, String> patients) {
    }
}

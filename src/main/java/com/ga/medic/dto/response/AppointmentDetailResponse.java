package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ga.medic.enums.AppointmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Detailed appointment information including the doctor's profile")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentDetailResponse(

        @Schema(description = "Optimistic-lock version; send this value when rescheduling", example = "0")
        Long version,

        @Schema(description = "Date of the appointment", example = "05/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate appointmentDate,

        @Schema(description = "Start time", example = "10:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "End time", example = "10:30")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Schema(description = "Consultation fee charged at the time of booking", example = "25.00")
        BigDecimal bookingFee,

        @Schema(description = "Appointment status", example = "BOOKED")
        AppointmentStatusEnum status,

        @Schema(description = "Reason for the appointment")
        String reason,

        @Schema(description = "Reason for cancellation, if cancelled")
        String cancelledReason,

        @Schema(description = "Doctor's full name", example = "Dr. Mariam Ali")
        String doctorName,

        @Schema(description = "Doctor's profile image URL")
        String doctorImageUrl,

        @Schema(description = "Doctor's qualification", example = "MBBS, MD")
        String doctorQualification,

        @Schema(description = "Doctor's medical specialization details")
        SpecializationResponse specialization,

        @Schema(description = "Years of professional experience", example = "10")
        Integer doctorYearsOfExperience,

        @Schema(description = "Hospital affiliation", example = "Manama Medical Center")
        String doctorHospitalAffiliation,

        @Schema(description = "Clinic address", example = "Building 45, Road 3621, Manama")
        String doctorClinicAddress,

        @Schema(description = "Medical license number", example = "MD10001")
        String doctorLicenseNumber,

        @Schema(description = "Professional biography")
        String doctorBio
) {
}

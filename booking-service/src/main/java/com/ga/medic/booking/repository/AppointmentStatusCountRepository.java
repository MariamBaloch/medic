package com.ga.medic.booking.repository;

import com.ga.medic.booking.enums.AppointmentStatusEnum;
import com.ga.medic.booking.model.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface AppointmentStatusCountRepository {

    Map<AppointmentStatusEnum, Long> countByStatus(Specification<Appointment> specification);
}

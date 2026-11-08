package com.ga.medic.repository;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface AppointmentStatusCountRepository {

    Map<AppointmentStatusEnum, Long> countByStatus(Specification<Appointment> specification);
}

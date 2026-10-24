package com.ga.medic.repository;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;

@Repository
public class AppointmentStatusCountRepositoryImpl implements AppointmentStatusCountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<AppointmentStatusEnum, Long> countByStatus(Specification<Appointment> specification) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
        Root<Appointment> appointment = query.from(Appointment.class);

        Predicate predicate = specification.toPredicate(appointment, query, criteriaBuilder);
        query.multiselect(
                        appointment.get("status").alias("status"),
                        criteriaBuilder.count(appointment).alias("count"))
                .groupBy(appointment.get("status"));

        if (predicate != null) {
            query.where(predicate);
        }

        Map<AppointmentStatusEnum, Long> counts = new EnumMap<>(AppointmentStatusEnum.class);
        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            counts.put(row.get("status", AppointmentStatusEnum.class), row.get("count", Long.class));
        }
        return counts;
    }
}

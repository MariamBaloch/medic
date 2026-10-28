package com.ga.medic.repository;

import com.ga.medic.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {

    List<Specialization> findAllByOrderByNameAsc();

    List<Specialization> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByNameAsc(
            String name,
            String description
    );
}

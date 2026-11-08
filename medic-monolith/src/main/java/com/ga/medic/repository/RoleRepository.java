package com.ga.medic.repository;

import com.ga.medic.enums.RoleEnum;
import com.ga.medic.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleEnum roleEnum);
}
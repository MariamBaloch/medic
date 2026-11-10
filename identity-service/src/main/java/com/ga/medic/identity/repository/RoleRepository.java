package com.ga.medic.identity.repository;

import com.ga.medic.identity.enums.RoleEnum;
import com.ga.medic.identity.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleEnum roleEnum);
}
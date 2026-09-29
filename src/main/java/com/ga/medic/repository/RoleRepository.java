package com.ga.medic.repository;

import com.ga.medic.enums.RoleEnum;
import com.ga.medic.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role getByName(RoleEnum name);
}
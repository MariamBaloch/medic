package com.ga.medic.service;

import com.ga.medic.enums.AuditAction;
import com.ga.medic.enums.AuditEntityType;
import com.ga.medic.model.AuditLog;
import com.ga.medic.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Saves an audit entry describing an action performed on an entity.
     *
     * @param username the username associated with the action
     * @param action the recorded action
     * @param entityType the type of entity affected
     * @param description details about the recorded action
     */
    public void log(String username, AuditAction action, AuditEntityType entityType, String description) {
        auditLogRepository.save(new AuditLog(username, action, entityType, description));
    }

    /**
     * Returns all audit entries ordered from newest to oldest.
     *
     * @return the audit entries ordered by timestamp descending
     */
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}

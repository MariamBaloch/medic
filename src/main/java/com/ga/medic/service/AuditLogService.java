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

    public void log(String username, AuditAction action, AuditEntityType entityType, String description) {
        auditLogRepository.save(new AuditLog(username, action, entityType, description));
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
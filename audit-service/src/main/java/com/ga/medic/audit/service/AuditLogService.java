package com.ga.medic.audit.service;

import com.ga.medic.audit.dto.AuditLogResponse;
import com.ga.medic.audit.enums.AuditAction;
import com.ga.medic.audit.enums.AuditEntityType;
import com.ga.medic.audit.model.AuditLog;
import com.ga.medic.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getUsername(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getDescription(),
                        log.getTimestamp()
                ))
                .toList();
    }

    @Transactional
    public void record(String username, AuditAction action, AuditEntityType entityType, String description) {
        auditLogRepository.save(new AuditLog(username, action, entityType, description));
    }
}

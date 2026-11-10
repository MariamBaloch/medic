package com.ga.medic.identity.aspect;

import com.ga.medic.identity.annotation.AuditLogger;
import com.ga.medic.common.event.EventTypes;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.identity.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuthenticatedUser authenticatedUser;
    private final OutboxService outboxService;

    @AfterReturning(pointcut = "@annotation(auditLogger)")
    public void logAudit(JoinPoint joinPoint, AuditLogger auditLogger) {
        try {
            String username = authenticatedUser.getName();
            outboxService.appendIndependent(
                    EventTypes.AUDIT_EVENT_RECORDED,
                    "audit",
                    auditLogger.entityType().name(),
                    OutboxService.payload(
                            "username", username,
                            "action", auditLogger.action().name(),
                            "entityType", auditLogger.entityType().name(),
                            "description", auditLogger.description()
                    )
            );
        } catch (Exception ex) {
            log.error("Failed to record audit for {}.{}: {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    ex.getMessage(),
                    ex);
        }
    }
}

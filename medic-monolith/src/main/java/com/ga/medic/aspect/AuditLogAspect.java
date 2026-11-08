package com.ga.medic.aspect;

import com.ga.medic.annotation.AuditLogger;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuthenticatedUser authenticatedUser;
    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditLogger)")
    public void logAudit(JoinPoint joinPoint, AuditLogger auditLogger) {

        auditLogService.log(authenticatedUser.get().user().getFullName(), auditLogger.action(), auditLogger.entityType(), auditLogger.description());
    }

}
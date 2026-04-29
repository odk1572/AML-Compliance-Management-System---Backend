package com.app.aml.annotation;


import com.app.aml.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditService;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAfterAction(JoinPoint joinPoint, AuditAction auditAction, Object result) {

        UUID entityId = findEntityId(joinPoint.getArgs());

        auditService.log(
                null,
                auditAction.category(),
                auditAction.action(),
                auditAction.entityType(),
                entityId,
                null,
                result
        );
    }

    private UUID findEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) return (UUID) arg;
        }
        return null;
    }
}
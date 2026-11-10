package com.ga.medic.identity.annotation;

import com.ga.medic.identity.enums.AuditAction;
import com.ga.medic.identity.enums.AuditEntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogger {

    AuditAction action();

    AuditEntityType entityType();

    String description();
}
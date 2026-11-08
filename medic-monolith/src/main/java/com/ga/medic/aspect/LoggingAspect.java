package com.ga.medic.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.ga.medic.controller.*.*(..))")
    public void logMethodCall(JoinPoint joinPoint) {
        log.info("Calling method ==> {}", joinPoint.getSignature().getName());
    }
}
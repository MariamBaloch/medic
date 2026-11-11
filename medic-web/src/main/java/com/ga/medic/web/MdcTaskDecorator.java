package com.ga.medic.web;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/** Runs async work with the caller's MDC, then restores the worker's MDC. */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable originalTask) {
        Map<String, String> submittingThreadContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> workerThreadExistingContext = MDC.getCopyOfContextMap();

            if (submittingThreadContext != null) {
                MDC.setContextMap(submittingThreadContext);
            } else {
                MDC.clear();
            }
            try {
                originalTask.run();
            } finally {
                if (workerThreadExistingContext != null) {
                    MDC.setContextMap(workerThreadExistingContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}

package com.ga.medic.notification.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class NotificationExecutorConfig {

    private static TaskDecorator mdcCopyingTaskDecorator() {
        return originalTask -> {
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
        };
    }

    @Bean(name = "notificationEmailExecutor", destroyMethod = "shutdown")
    public ExecutorService notificationEmailExecutor() {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = task -> new Thread(task, "notification-email-" + threadNumber.incrementAndGet());
        TaskDecorator mdcDecorator = mdcCopyingTaskDecorator();

        return new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            public void execute(@NonNull Runnable originalTask) {
                Runnable decoratedTask = mdcDecorator.decorate(originalTask);
                super.execute(decoratedTask);
            }
        };
    }
}

package com.ga.medic.notification.config;

import com.ga.medic.web.MdcTaskDecorator;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class NotificationExecutorConfig {

    @Bean(name = "notificationEmailExecutor", destroyMethod = "shutdown")
    public ExecutorService notificationEmailExecutor() {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = task -> new Thread(task, "notification-email-" + threadNumber.incrementAndGet());
        TaskDecorator mdcDecorator = new MdcTaskDecorator();

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

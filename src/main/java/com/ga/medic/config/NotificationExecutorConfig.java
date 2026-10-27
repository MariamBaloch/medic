package com.ga.medic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class NotificationExecutorConfig {

    @Bean(name = "notificationEmailExecutor", destroyMethod = "shutdown")
    public ExecutorService notificationEmailExecutor() {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = task -> new Thread(task, "notification-email-" + threadNumber.incrementAndGet());

        return new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}

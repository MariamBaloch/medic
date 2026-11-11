package com.ga.medic.messaging.consumer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Enables shared processed-event idempotency when app.processed-event.enabled=true
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.processed-event.enabled", havingValue = "true")
@EntityScan(basePackageClasses = ProcessedEvent.class)
@EnableJpaRepositories(basePackageClasses = ProcessedEventRepository.class)
@ComponentScan(basePackageClasses = ProcessedEventRetentionJob.class)
public class ProcessedEventAutoConfiguration {
}

package com.ga.medic.messaging.consumer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Enables shared processed-event idempotency when app.processed-event.enabled=true
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.processed-event.enabled", havingValue = "true")
@AutoConfigurationPackage(basePackageClasses = ProcessedEvent.class)
@ComponentScan(basePackageClasses = ProcessedEventRetentionJob.class)
public class ProcessedEventAutoConfiguration {
}

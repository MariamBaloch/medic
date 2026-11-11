package com.ga.medic.messaging.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Enables shared outbox persistence and publisher when app.outbox.enabled=true
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
@ComponentScan(basePackageClasses = OutboxService.class)
public class OutboxMessagingAutoConfiguration {
}

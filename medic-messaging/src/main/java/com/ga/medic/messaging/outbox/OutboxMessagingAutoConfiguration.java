package com.ga.medic.messaging.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Enables shared outbox persistence and publisher when app.outbox.enabled=true
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
@EntityScan(basePackageClasses = OutboxEvent.class)
@EnableJpaRepositories(basePackageClasses = OutboxEventRepository.class)
@ComponentScan(basePackageClasses = OutboxService.class)
public class OutboxMessagingAutoConfiguration {
}

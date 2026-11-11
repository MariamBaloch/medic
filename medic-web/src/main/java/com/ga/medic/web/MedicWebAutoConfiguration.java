package com.ga.medic.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Wires correlation filter, page mapper, and RestClient correlation header. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MedicWebAutoConfiguration {

    /** Puts correlation id on the request MDC (and response header). */
    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /** Turns a Spring Page into a PageResponse. */
    @Bean
    @ConditionalOnMissingBean
    public PageMapper pageMapper() {
        return new PageMapper();
    }

    /** Adds correlation id to outbound RestClient calls. */
    @Bean
    public RestClientCustomizer correlationIdRestClientCustomizer() {
        return builder -> builder.requestInterceptor(new CorrelationIdRestClientInterceptor());
    }
}

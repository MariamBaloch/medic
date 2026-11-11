package com.ga.medic.web;

import com.ga.medic.common.api.CorrelationId;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** Copies correlation id from MDC onto outbound HTTP calls. */
public class CorrelationIdRestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(CorrelationId.HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}

package com.ga.medic.common.api;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]{8,128}");

    private CorrelationId() {
    }

    /**
     * Returns the incoming correlation ID when it is safe to log and send forward,
     * otherwise a new UUID.
     */
    public static String resolve(String incoming) {
        return isValid(incoming) ? incoming : UUID.randomUUID().toString();
    }

    public static boolean isValid(String correlationId) {
        return correlationId != null && SAFE_VALUE.matcher(correlationId).matches();
    }
}

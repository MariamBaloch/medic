package com.ga.medic.config;

import com.ga.medic.security.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the RestClient used for communication with the Identity Service.
 * Sets the service base URL and internal API key.
 * The correlation ID is automatically propagated through the registered RestClient interceptor.
 */

@Configuration
public class IdentityClientConfig {

    @Bean
    RestClient identityRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.identity.url}") String identityUrl,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        return restClientBuilder
                .baseUrl(identityUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }
}

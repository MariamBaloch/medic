package com.ga.medic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IdentityClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Key";

    @Bean
    RestClient identityRestClient(
            @Value("${app.identity.url}") String identityUrl,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        return RestClient.builder()
                .baseUrl(identityUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .build();
    }
}

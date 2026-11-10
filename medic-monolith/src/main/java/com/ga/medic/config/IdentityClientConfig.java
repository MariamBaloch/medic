package com.ga.medic.config;

import com.ga.medic.security.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IdentityClientConfig {

    @Bean
    RestClient identityRestClient(
            @Value("${app.identity.url}") String identityUrl,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        return RestClient.builder()
                .baseUrl(identityUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }
}

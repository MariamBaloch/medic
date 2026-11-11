package com.ga.medic.identity.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI medicOpenApi(
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${app.public-base-path:/api/identity}") String publicBasePath
    ) {
        return new OpenAPI()
                .info(new Info().title("Medic Identity API").version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addServersItem(new Server()
                        .url(trimTrailingSlash(appBaseUrl) + publicBasePath)
                        .description("Via gateway"));
    }

    @Bean
    @Primary
    @Lazy(false)
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setTagsSorter("alpha");
        config.setOperationsSorter("alpha");
        return config;
    }

    private static String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

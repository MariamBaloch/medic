package com.ga.medic.booking.cucumber;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.security.TokenBlacklistService;
import com.ga.medic.security.TokenVersionService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(properties = {
        "spring.datasource.hikari.connection-init-sql=",
        "spring.jpa.properties.hibernate.default_schema=",
        "management.health.redis.enabled=false",
        "management.health.kafka.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @MockitoBean
    InternalIdentityClient internalIdentityClient;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    TokenVersionService tokenVersionService;
}

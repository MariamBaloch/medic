package com.ga.medic.booking.concurrent;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.security.TokenBlacklistService;
import com.ga.medic.security.TokenVersionService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.health.redis.enabled=false",
        "management.health.kafka.enabled=false"
})
@ActiveProfiles("integration")
public class ConcurrencySpringConfiguration {

    @MockitoBean
    InternalIdentityClient internalIdentityClient;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    TokenVersionService tokenVersionService;
}

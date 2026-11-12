package com.ga.medic.booking.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class ScheduleCacheConfiguration {

    private static final Duration SCHEDULE_CACHE_TTL = Duration.ofHours(24);

    @Bean
    @ConditionalOnProperty(name = "app.schedule-cache.enabled", havingValue = "true", matchIfMissing = true)
    public RedisCacheManager scheduleRedisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(SCHEDULE_CACHE_TTL)
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.schedule-cache.enabled", havingValue = "false")
    public CacheManager scheduleNoOpCacheManager() {
        return new NoOpCacheManager();
    }
}

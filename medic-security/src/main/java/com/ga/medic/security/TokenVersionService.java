package com.ga.medic.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenVersionService {

    static final String KEY_PREFIX = "token-ver:";

    private final StringRedisTemplate redisTemplate;

    public void store(Long userId, int version) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, String.valueOf(version));
    }

    public boolean matches(Long userId, int expectedVersion) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (stored == null) {
            return false;
        }
        try {
            return Integer.parseInt(stored) == expectedVersion;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}

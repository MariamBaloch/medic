package com.ga.medic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Stores the token ID in Redis only for the token's remaining lifetime.
     *
     * @param jti the JWT ID to blacklist
     * @param expiresAt the token's expiration time
     */
    public void blacklist(String jti, Date expiresAt) {
        long remainingMs = expiresAt.getTime() - System.currentTimeMillis();
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set("blacklist:" + jti, "1", Duration.ofMillis(remainingMs));
        }
    }

    /**
     * Checks whether Redis contains the given token ID in the blacklist.
     *
     * @param jti the JWT ID to check
     * @return true if the token ID is blacklisted, otherwise false
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }
}

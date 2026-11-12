package com.ga.medic.booking.service;

import com.ga.medic.booking.event.DoctorScheduleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tracks a per-doctor schedule version so a schedule change stops referencing older cached weeks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorScheduleCacheVersionService {

    private static final String VERSION_KEY_PREFIX = "doctor-schedule-version:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.schedule-cache.enabled:true}")
    private boolean scheduleCacheEnabled;

    /**
     * Returns the current schedule version for a doctor.
     *
     * @param doctorId the doctor's profile ID
     * @return the current version, or 0 when caching is disabled
     */
    public long getVersion(Long doctorId) {
        if (!scheduleCacheEnabled) {
            return 0L;
        }

        String version = redisTemplate.opsForValue().get(versionKey(doctorId));
        return version == null ? 0L : Long.parseLong(version);
    }

    /**
     * Advances the doctor's version only after the schedule change commits.
     *
     * @param event the committed schedule change
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void incrementAfterScheduleChange(DoctorScheduleChangedEvent event) {
        log.info("Received schedule change event for doctor {}", event.doctorId());

        if (!scheduleCacheEnabled) {
            log.info("Schedule caching is disabled");
            return;
        }

        Long version = redisTemplate.opsForValue().increment(versionKey(event.doctorId()));
        log.info("Updated schedule version to {}", version);
    }

    private String versionKey(Long doctorId) {
        return VERSION_KEY_PREFIX + doctorId;
    }
}

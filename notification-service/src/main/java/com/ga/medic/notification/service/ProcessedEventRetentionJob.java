package com.ga.medic.notification.service;

import com.ga.medic.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Deletes processed-event markers older than the configured retention window
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventRetentionJob {

    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.processed-event.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "${app.processed-event.retention-cron:0 15 3 * * *}")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = processedEventRepository.deleteByProcessedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} processed_event rows older than {} days (cutoff={})",
                    deleted, retentionDays, cutoff);
        }
    }
}
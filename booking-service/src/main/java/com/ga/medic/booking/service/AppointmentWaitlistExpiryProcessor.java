package com.ga.medic.booking.service;

import com.ga.medic.booking.enums.WaitlistStatusEnum;
import com.ga.medic.booking.repository.AppointmentWaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentWaitlistExpiryProcessor {

    private final AppointmentWaitlistEntryRepository repository;
    private final AppointmentWaitlistService waitlistService;

    @Scheduled(fixedDelay = 30_000)
    @SchedulerLock(name = "waitlistOfferExpiry", lockAtLeastFor = "PT20S", lockAtMostFor = "PT2M")
    public void processExpiredOffers() {
        for (Long entryId : repository.findExpiredOfferIds(
                WaitlistStatusEnum.OFFERED, Instant.now(), PageRequest.of(0, 100))) {
            try {
                waitlistService.expireOffer(entryId);
            } catch (RuntimeException exception) {
                log.error("Could not process expired appointment waitlist offer: waitlistEntryId={}", entryId, exception);
            }
        }
    }
}

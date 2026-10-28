package com.ga.medic.service;

import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.repository.AppointmentWaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

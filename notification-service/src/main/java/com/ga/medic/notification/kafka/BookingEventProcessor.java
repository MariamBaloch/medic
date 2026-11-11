package com.ga.medic.notification.kafka;

import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.messaging.consumer.ProcessedEvent;
import com.ga.medic.messaging.consumer.ProcessedEventRepository;
import com.ga.medic.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventProcessor {

    private final ProcessedEventRepository processedEventRepository;
    private final BookingNotificationFactory notificationFactory;
    private final NotificationService notificationService;

    @Transactional
    public void process(EventEnvelope event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Event envelope missing eventId");
        }
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Skipping already processed eventId={}", event.eventId());
            return;
        }

        List<NotificationDraft> drafts = notificationFactory.draftsFor(event);
        for (NotificationDraft draft : drafts) {
            if (draft.userId() == null || draft.email() == null || draft.email().isBlank()) {
                log.warn(
                        "Skipping notification draft with missing recipient: eventId={}, eventType={}, userId={}, emailBlank={}",
                        event.eventId(),
                        event.eventType(),
                        draft.userId(),
                        draft.email() == null || draft.email().isBlank()
                );
                continue;
            }
            notificationService.createAndSend(
                    draft.userId(),
                    draft.email(),
                    draft.fullName() == null || draft.fullName().isBlank() ? draft.email() : draft.fullName(),
                    draft.type(),
                    draft.action(),
                    draft.title(),
                    draft.message(),
                    false,
                    draft.relatedEntityId()
            );
        }

        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(event.eventId());
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}

package com.ga.medic.service;

import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.mapper.NotificationMapper;
import com.ga.medic.model.Notification;
import com.ga.medic.model.User;
import com.ga.medic.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    // Map to hold userId and all active SseEmitters for that user for multiple connections
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }
        return emitter;
    }

    public void sendNotification(Long userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        log.info("Sending notification to userId {}: {}", userId, notification);

        if (userEmitters == null) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
                log.info("Notification sent to userId {}: {}", userId, notification);
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null) {
            userEmitters.remove(emitter);

            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    public void createAndSend(User user, NotificationType type, NotificationAction action, String title, String message, boolean read, Long relatedEntityId
    ) {
        Notification notification = notificationMapper.toNotification(user, type, action, title, message, read, relatedEntityId);
        notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(notification);
        sendNotification(user.getId(), response);
    }
}
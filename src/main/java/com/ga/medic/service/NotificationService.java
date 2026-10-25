package com.ga.medic.service;

import com.ga.medic.dto.response.MarkAllNotificationsReadResponse;
import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.dto.response.UnreadNotificationCountResponse;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.event.NotificationDispatchEvent;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.NotificationMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.Notification;
import com.ga.medic.model.User;
import com.ga.medic.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationMapper notificationMapper;
    private final PageMapper pageMapper;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    @Qualifier("notificationEmailExecutor")
    private final ExecutorService notificationEmailExecutor;
    // Map to hold userId and all active SseEmitters for that user for multiple connections
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, Pageable pageable, boolean unreadOnly) {
        if (pageable == null || pageable.isUnpaged() || pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : NEWEST_FIRST_SORT;
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findUnreadByUserId(userId, pageRequest)
                : notificationRepository.findByUserId(userId, pageRequest);
        return pageMapper.toResponse(notifications, notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        return new UnreadNotificationCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new InformationNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllAsRead(Long userId) {
        return new MarkAllNotificationsReadResponse(notificationRepository.markAllReadByUserId(userId));
    }

    /**
     * Opens a server-sent-events connection and tracks it for the user's live notifications.
     *
     * @param userId the ID of the user subscribing to notifications
     * @return the event stream connected to the user
     */
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

    /**
     * Sends a notification to every active event-stream connection for the user.
     *
     * @param userId       the ID of the notification recipient
     * @param notification the notification to send
     */
    public void sendNotification(Long userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters == null) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
                log.debug("SSE notification sent: userId={}, notificationId={}", userId, notification.id());
            } catch (IOException | RuntimeException exception) {
                log.warn("Removing failed SSE connection: userId={}, notificationId={}",
                        userId, notification.id(), exception);
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Removes a closed event stream and drops the user's entry when no streams remain.
     *
     * @param userId  the ID of the stream's user
     * @param emitter the event stream to remove
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null) {
            userEmitters.remove(emitter);

            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Persists a notification in the caller's transaction and schedules delivery after commit.
     *
     * @param user            the notification recipient
     * @param type            the notification category
     * @param action          the action associated with the notification
     * @param title           the notification title
     * @param message         the notification body
     * @param read            whether the notification is initially marked as read
     * @param relatedEntityId the ID of the related entity, if any
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createAndSend(User user, NotificationType type, NotificationAction action, String title,
                              String message, boolean read, Long relatedEntityId) {
        Notification notification = notificationMapper.toNotification(user, type, action, title, message, read, relatedEntityId);
        notification = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(notification);
        eventPublisher.publishEvent(new NotificationDispatchEvent(
                user.getId(), user.getEmail(), user.getFullName(), response
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(NotificationDispatchEvent event) {
        sendNotification(event.userId(), event.notification());

        try {
            notificationEmailExecutor.execute(() -> sendEmailNotification(event));
            log.info("Submitted notification email: notificationId={}, userId={}",
                    event.notification().id(), event.userId());
        } catch (RejectedExecutionException exception) {
            log.error("Could not submit notification email: notificationId={}, userId={}",
                    event.notification().id(), event.userId(), exception);
        }
    }

    private void sendEmailNotification(NotificationDispatchEvent event) {
        NotificationResponse notification = event.notification();
        try {
            emailService.sendTemplateEmail(
                    event.recipientEmail(),
                    notification.title(),
                    "email/notification",
                    Map.of(
                            "fullName", event.recipientName(),
                            "title", notification.title(),
                            "message", notification.message(),
                            "type", notification.type(),
                            "action", notification.action(),
                            "relatedEntityId", notification.relatedEntityId() == null
                                    ? "" : notification.relatedEntityId()
                    )
            );
            log.info("Notification email sent: notificationId={}, userId={}", notification.id(), event.userId());
        } catch (RuntimeException exception) {
            log.warn("Notification email failed: notificationId={}, userId={}",
                    notification.id(), event.userId(), exception);
        }
    }
}

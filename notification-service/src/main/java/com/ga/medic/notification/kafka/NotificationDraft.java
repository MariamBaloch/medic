package com.ga.medic.notification.kafka;

import com.ga.medic.notification.enums.NotificationAction;
import com.ga.medic.notification.enums.NotificationType;

public record NotificationDraft(
        Long userId,
        String email,
        String fullName,
        NotificationType type,
        NotificationAction action,
        String title,
        String message,
        Long relatedEntityId
) {
}

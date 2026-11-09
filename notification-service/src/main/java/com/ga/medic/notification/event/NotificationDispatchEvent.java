package com.ga.medic.notification.event;

import com.ga.medic.notification.dto.NotificationResponse;

public record NotificationDispatchEvent(
        Long userId,
        String recipientEmail,
        String recipientName,
        NotificationResponse notification
) {
}

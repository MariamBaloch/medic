package com.ga.medic.event;

import com.ga.medic.dto.response.NotificationResponse;

public record NotificationDispatchEvent(
        Long userId,
        String recipientEmail,
        String recipientName,
        NotificationResponse notification
) {
}

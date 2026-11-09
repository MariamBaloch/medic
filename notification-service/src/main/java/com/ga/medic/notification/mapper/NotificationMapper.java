package com.ga.medic.notification.mapper;

import com.ga.medic.notification.dto.NotificationResponse;
import com.ga.medic.notification.enums.NotificationAction;
import com.ga.medic.notification.enums.NotificationType;
import com.ga.medic.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "type", expression = "java(notification.getType().name())")
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    Notification toNotification(
            Long userId,
            NotificationType type,
            NotificationAction action,
            String title,
            String message,
            boolean read,
            Long relatedEntityId
    );
}

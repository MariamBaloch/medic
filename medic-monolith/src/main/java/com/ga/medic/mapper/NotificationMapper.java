package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.model.Notification;
import com.ga.medic.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", ignore = true)
    Notification toNotification(User user, NotificationType type, NotificationAction action, String title, String message, boolean read, Long relatedEntityId);
}
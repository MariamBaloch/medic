package com.ga.medic.notification.controller;

import com.ga.medic.notification.dto.MarkAllNotificationsReadResponse;
import com.ga.medic.notification.dto.NotificationResponse;
import com.ga.medic.notification.dto.UnreadNotificationCountResponse;
import com.ga.medic.notification.service.NotificationService;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping
    @Operation(summary = "List notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Return unread notifications only")
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotifications(authenticatedUser.getUserId(), pageable, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount(authenticatedUser.getUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead(authenticatedUser.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(authenticatedUser.getUserId(), notificationId));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return notificationService.subscribe(authenticatedUser.getUserId());
    }
}

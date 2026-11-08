package com.ga.medic.controller;

import com.ga.medic.dto.response.MarkAllNotificationsReadResponse;
import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.dto.response.UnreadNotificationCountResponse;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUser authenticatedUser;

    @Operation(summary = "List notifications",
            description = "Returns the authenticated user's notifications, newest first by default. Set unreadOnly=true to show unread notifications only.")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Return unread notifications only", example = "false")
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotifications(
                authenticatedUser.getUserId(), pageable, unreadOnly));
    }

    @Operation(summary = "Get my unread notification count")
    @ApiResponse(responseCode = "200", description = "Unread notification count returned")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount(authenticatedUser.getUserId()));
    }

    @Operation(summary = "Mark all my notifications as read")
    @ApiResponse(responseCode = "200", description = "Notifications marked as read")
    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead(authenticatedUser.getUserId()));
    }

    @Operation(summary = "Mark one of my notifications as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found for this user")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(authenticatedUser.getUserId(), notificationId));
    }

    @Operation(summary = "Subscribe to notifications",
            description = "Returns a Server-Sent Events (SSE) stream for real-time notifications.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return notificationService.subscribe(authenticatedUser.getUserId());
    }
}

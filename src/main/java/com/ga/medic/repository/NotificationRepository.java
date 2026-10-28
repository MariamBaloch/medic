package com.ga.medic.repository;

import com.ga.medic.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    @Query("select notification from Notification notification " +
            "where notification.user.id = :userId and notification.read = false")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long notificationId, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification notification set notification.read = true " +
            "where notification.user.id = :userId and notification.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}

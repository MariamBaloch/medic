package com.ga.medic.notification.repository;

import com.ga.medic.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    @Query("select n from Notification n where n.userId = :userId and n.read = false")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long notificationId, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}

package com.ga.medic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AppointmentSlotLockService {

    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JdbcTemplate jdbcTemplate;

    /**
     * Acquires a transaction-level lock for a specific appointment slot.
     * Concurrent transactions requesting the same slot must wait until the
     * current transaction completes.
     *
     * @param doctorId  the doctor profile ID
     * @param date      the appointment date
     * @param startTime the appointment slot start time
     * @throws IllegalStateException if no transaction is active or the database
     *                               is not supported
     */
    public void acquire(Long doctorId, LocalDate date, LocalTime startTime) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Appointment slot locks must be acquired inside an active transaction");
        }

        String key = "appointment-slot:" + doctorId + ":" + date + ":"
                + startTime.format(SLOT_TIME);

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            String database = connection.getMetaData().getDatabaseProductName();

            if ("H2".equalsIgnoreCase(database)) {
                return null;
            }

            if (!"PostgreSQL".equalsIgnoreCase(database)) {
                throw new IllegalStateException(
                        "Appointment slot locking requires PostgreSQL");
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                statement.setString(1, key);
                statement.execute();
            }

            return null;
        });
    }

    /**
     * Acquires locks for multiple appointment slots in a consistent order
     * to reduce the risk of deadlocks.
     *
     * @param slots the appointment slots to lock
     */
    public void acquireAll(Collection<SlotKey> slots) {
        slots.stream()
                .distinct()
                .sorted(Comparator.comparing(SlotKey::doctorId)
                        .thenComparing(SlotKey::date)
                        .thenComparing(SlotKey::startTime))
                .forEach(slot -> acquire(
                        slot.doctorId(),
                        slot.date(),
                        slot.startTime()));
    }

    /**
     * Identifies an appointment slot by its doctor, date, and start time.
     *
     * @param doctorId  the doctor profile ID
     * @param date      the appointment date
     * @param startTime the appointment slot start time
     */
    public record SlotKey(Long doctorId, LocalDate date, LocalTime startTime) {
    }
}
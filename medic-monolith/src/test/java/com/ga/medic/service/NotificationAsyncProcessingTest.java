package com.ga.medic.service;

import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.model.Role;
import com.ga.medic.model.User;
import com.ga.medic.repository.NotificationRepository;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class NotificationAsyncProcessingTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    @Qualifier("notificationEmailExecutor")
    private ExecutorService notificationEmailExecutor;

    @MockitoBean
    private EmailService emailService;

    @Test
    void notificationsAreCommittedBeforeConcurrentEmailTasksAndEmailFailureDoesNotUndoThem() throws Exception {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) notificationEmailExecutor;
        assertEquals(2, threadPool.getCorePoolSize());
        assertEquals(4, threadPool.getMaximumPoolSize());
        assertTrue(threadPool.getQueue() instanceof ArrayBlockingQueue<?>);

        CountDownLatch emailTasksStarted = new CountDownLatch(2);
        CountDownLatch releaseEmailTasks = new CountDownLatch(1);
        CountDownLatch emailTasksFinished = new CountDownLatch(2);
        AtomicBoolean transactionCommitted = new AtomicBoolean(false);
        AtomicBoolean emailStartedBeforeCommit = new AtomicBoolean(false);
        AtomicBoolean emailRanInTransaction = new AtomicBoolean(false);
        Set<String> workerThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger unexpectedNotificationCountAtEmail = new AtomicInteger(-1);
        SseEmitter rescheduleEmitter = mock(SseEmitter.class);

        doAnswer(invocation -> {
            String recipient = invocation.getArgument(0);
            String title = invocation.getArgument(1);
            long committedNotifications = jdbcTemplate.queryForObject("""
                    select count(*)
                    from notification notification
                    join users recipient_user on recipient_user.id = notification.user_id
                    where recipient_user.email = ? and notification.title = ?
                    """, Long.class, recipient, title);

            if (!transactionCommitted.get()) {
                emailStartedBeforeCommit.set(true);
            }
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                emailRanInTransaction.set(true);
            }
            if (committedNotifications != 1) {
                unexpectedNotificationCountAtEmail.set((int) committedNotifications);
            }
            workerThreads.add(Thread.currentThread().getName());
            emailTasksStarted.countDown();

            try {
                if (!releaseEmailTasks.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to finish the async email test");
                }
                throw new IllegalStateException("Simulated SMTP failure");
            } finally {
                emailTasksFinished.countDown();
            }
        }).when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        List<User> recipients = transaction.execute(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public int getOrder() {
                    return Ordered.HIGHEST_PRECEDENCE;
                }

                @Override
                public void afterCommit() {
                    transactionCommitted.set(true);
                }
            });

            Role patientRole = findOrCreateRole(RoleEnum.PATIENT);
            Role doctorRole = findOrCreateRole(RoleEnum.DOCTOR);
            User patient = saveNotificationRecipient(patientRole, "patient");
            User doctor = saveNotificationRecipient(doctorRole, "doctor");
            registerEmitter(doctor.getId(), rescheduleEmitter);

            notificationService.createAndSend(patient, NotificationType.APPOINTMENT, NotificationAction.BOOKED,
                    "Async patient notification", "Patient booking notification", false, 201L);
            notificationService.createAndSend(doctor, NotificationType.APPOINTMENT, NotificationAction.RESCHEDULED,
                    "Async doctor notification", "Doctor reschedule notification", false, 201L);

            assertEquals(2, emailTasksStarted.getCount(), "Email tasks must not start before transaction commit");
            verifyNoInteractions(rescheduleEmitter);
            return List.of(patient, doctor);
        });

        assertNotNull(recipients);
        try {
            assertTrue(emailTasksStarted.await(10, TimeUnit.SECONDS), "Both email tasks should run concurrently");
            assertTrue(transactionCommitted.get(), "The transaction's after-commit callback should have run");
            assertFalse(emailStartedBeforeCommit.get(), "Email dispatch must happen after commit");
            assertFalse(emailRanInTransaction.get(), "Email work must run outside the caller's transaction");
            assertEquals(-1, unexpectedNotificationCountAtEmail.get(),
                    "Both notification records should be visible to workers after commit");
            assertEquals(2, workerThreads.size(), "The two tasks should run on separate pool threads");
            assertTrue(workerThreads.stream().allMatch(name -> name.startsWith("notification-email-")));

            ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventCaptor =
                    ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
            verify(rescheduleEmitter).send(sseEventCaptor.capture());
            assertTrue(sseEventCaptor.getValue().build().stream()
                            .map(ResponseBodyEmitter.DataWithMediaType::getData)
                            .anyMatch(data -> data instanceof NotificationResponse notification
                                    && notification.action() == NotificationAction.RESCHEDULED),
                    "Committed reschedules should reach SSE subscribers with the RESCHEDULED action");

            releaseEmailTasks.countDown();
            assertTrue(emailTasksFinished.await(10, TimeUnit.SECONDS));

            assertEquals(2, notificationRepository.count(), "Email failures must not roll back notifications");
            verify(emailService, times(2)).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
        } finally {
            releaseEmailTasks.countDown();
        }
    }

    private User saveNotificationRecipient(Role role, String prefix) {
        User user = new User();
        user.setRole(role);
        user.setEmail(prefix + "-" + UUID.randomUUID() + "@example.test");
        user.setPassword("test-only-password");
        user.setFirstName(prefix);
        user.setLastName("notification");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("5551234567");
        user.setStatus(UserStatusEnum.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private Role findOrCreateRole(RoleEnum roleEnum) {
        return roleRepository.findByName(roleEnum)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleEnum);
                    return roleRepository.save(role);
                });
    }

    @SuppressWarnings("unchecked")
    private void registerEmitter(Long userId, SseEmitter emitter) {
        Map<Long, Set<SseEmitter>> activeEmitters =
                (Map<Long, Set<SseEmitter>>) ReflectionTestUtils.getField(notificationService, "emitters");
        assertNotNull(activeEmitters);
        activeEmitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
    }
}

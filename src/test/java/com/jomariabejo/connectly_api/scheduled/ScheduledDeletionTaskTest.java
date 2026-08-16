package com.jomariabejo.connectly_api.scheduled;

import com.jomariabejo.connectly_api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScheduledDeletionTask}.
 *
 * <p>The task is a thin scheduled wrapper around
 * {@link UserService#checkAndDeleteScheduledUsers()}, so the tests only pin the two behaviors that
 * matter: it delegates to the service, and it swallows any exception the service throws (the
 * production method wraps the call in a try/catch that merely logs, so one bad run must never
 * bubble up into the scheduler thread). The {@code @Scheduled}/{@code @SchedulerLock} wiring is
 * container behavior and is deliberately out of scope here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledDeletionTask")
class ScheduledDeletionTaskTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private ScheduledDeletionTask task;

    @Nested
    @DisplayName("permanentlyDeleteScheduledUsers")
    class PermanentlyDeleteScheduledUsers {

        @Test
        @DisplayName("delegates the purge to userService.checkAndDeleteScheduledUsers")
        void delegatesToUserService() {
            when(userService.checkAndDeleteScheduledUsers()).thenReturn(3);

            task.permanentlyDeleteScheduledUsers();

            verify(userService).checkAndDeleteScheduledUsers();
        }

        @Test
        @DisplayName("swallows exceptions thrown by the service instead of propagating them")
        void swallowsServiceExceptions() {
            when(userService.checkAndDeleteScheduledUsers())
                    .thenThrow(new RuntimeException("database unavailable"));

            assertThatCode(() -> task.permanentlyDeleteScheduledUsers())
                    .doesNotThrowAnyException();
        }
    }
}

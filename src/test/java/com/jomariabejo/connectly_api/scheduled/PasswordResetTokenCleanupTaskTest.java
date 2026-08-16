package com.jomariabejo.connectly_api.scheduled;

import com.jomariabejo.connectly_api.service.PasswordResetTokenService;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link PasswordResetTokenCleanupTask}.
 *
 * <p>The task chains two deletions inside a single try/catch: password reset tokens first, then
 * verification tokens (the latter was added because
 * {@code VerificationTokenService.cleanupExpiredTokens()} had no {@code @Scheduled} caller of its
 * own and expired rows accumulated). Because one try block wraps both calls, a failure in the
 * first deletion means the second is never reached -- that coupling is pinned explicitly here so a
 * refactor that separates (or reorders) the two shows up as a test change. Exceptions are logged
 * and swallowed, never propagated to the scheduler thread.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenCleanupTask")
class PasswordResetTokenCleanupTaskTest {

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @InjectMocks
    private PasswordResetTokenCleanupTask task;

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("deletes expired password reset tokens, then expired verification tokens")
        void delegatesToBothServicesInOrder() {
            task.cleanupExpiredTokens();

            InOrder inOrder = inOrder(passwordResetTokenService, verificationTokenService);
            inOrder.verify(passwordResetTokenService).deleteExpiredTokens();
            inOrder.verify(verificationTokenService).cleanupExpiredTokens();
        }

        @Test
        @DisplayName("when the password reset cleanup throws, swallows it and skips the verification cleanup")
        void firstFailureSkipsSecondCall() {
            doThrow(new RuntimeException("delete failed"))
                    .when(passwordResetTokenService).deleteExpiredTokens();

            assertThatCode(() -> task.cleanupExpiredTokens())
                    .doesNotThrowAnyException();

            verifyNoInteractions(verificationTokenService);
        }

        @Test
        @DisplayName("when the verification cleanup throws, swallows it after both calls ran")
        void secondFailureIsSwallowed() {
            doThrow(new RuntimeException("delete failed"))
                    .when(verificationTokenService).cleanupExpiredTokens();

            assertThatCode(() -> task.cleanupExpiredTokens())
                    .doesNotThrowAnyException();

            verify(passwordResetTokenService).deleteExpiredTokens();
            verify(verificationTokenService).cleanupExpiredTokens();
        }
    }
}

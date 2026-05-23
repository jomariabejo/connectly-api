package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.config.AccountDeletionConfig;
import com.jomariabejo.connectly_api.exception.InvalidReactivationTokenException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final VerificationTokenRepository tokenRepository = mock(VerificationTokenRepository.class);
    private final RateLimitingService rateLimitingService = mock(RateLimitingService.class);
    private final UserService userService = new UserService(userRepository, tokenRepository, rateLimitingService);

    @Test
    void reactivationRequiresValidEmailToken() {
        when(rateLimitingService.isRateLimited("bad-token", AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION))
                .thenReturn(false);
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.reactivateAccount("bad-token"))
                .isInstanceOf(InvalidReactivationTokenException.class)
                .hasMessage("Invalid reactivation token");

        verify(rateLimitingService).recordAttempt("bad-token", AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void validReactivationClearsDeletionStateAndDeletesToken() {
        User user = new User();
        user.setDeletedAt(Date.from(Instant.now().minusSeconds(60)));
        user.setScheduledDeletionAt(Date.from(Instant.now().plus(AccountDeletionConfig.GRACE_PERIOD)));
        user.setActive(false);

        VerificationToken token = new VerificationToken();
        token.setToken("good-token");
        token.setUser(user);
        token.setExpiryDate(Date.from(Instant.now().plus(AccountDeletionConfig.GRACE_PERIOD)));

        when(rateLimitingService.isRateLimited("good-token", AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION))
                .thenReturn(false);
        when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));
        when(userRepository.save(user)).thenReturn(user);

        userService.reactivateAccount("good-token");

        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getScheduledDeletionAt()).isNull();
        assertThat(user.isActive()).isTrue();
        verify(tokenRepository).delete(token);
    }

    @Test
    void expiredReactivationTokenDoesNotMutateUserOrDeleteToken() {
        User user = new User();
        user.setDeletedAt(Date.from(Instant.now().minusSeconds(60)));
        user.setScheduledDeletionAt(Date.from(Instant.now().plus(AccountDeletionConfig.GRACE_PERIOD)));
        user.setActive(false);

        VerificationToken token = new VerificationToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiryDate(Date.from(Instant.now().minusSeconds(60)));

        when(rateLimitingService.isRateLimited("expired-token", AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION))
                .thenReturn(false);
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.reactivateAccount("expired-token"))
                .isInstanceOf(InvalidReactivationTokenException.class)
                .hasMessage("Reactivation token has expired");

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getScheduledDeletionAt()).isNotNull();
        assertThat(user.isActive()).isFalse();
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(tokenRepository, never()).delete(token);
    }
}

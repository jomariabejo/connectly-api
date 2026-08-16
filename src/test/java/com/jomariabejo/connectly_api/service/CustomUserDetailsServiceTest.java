package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.AccountDeletionScheduledException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 *
 * <p>The service mixes two injection styles: {@link UserRepository} arrives through the
 * constructor, while {@link UserService} is an {@code @Autowired(required = false)} <em>field</em>
 * that Spring may legitimately leave {@code null} (it exists only to break a circular dependency
 * in some contexts). {@code @InjectMocks} would blindly fill that field on every test, making the
 * {@code userService == null} guard untestable. So the service under test is built by hand with
 * {@code new CustomUserDetailsService(userRepository)} and the field is set explicitly via
 * {@link ReflectionTestUtils#setField} -- and one test builds a second instance where the field is
 * deliberately left {@code null} to pin the null-guard branch.
 *
 * <p>Fixtures always call {@link User#setAutoReactivationEnabled(boolean)} explicitly because
 * {@link User#isAutoReactivationEnabled()} unboxes a {@link Boolean} field and would otherwise
 * couple these tests to the entity's field initializer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    private static final String EMAIL = "someone@example.com";

    /** Fixed instants so the exception-payload assertions are fully deterministic. */
    private static final Date DELETED_AT = new Date(1_700_000_000_000L);
    private static final Date SCHEDULED_DELETION_AT =
            new Date(DELETED_AT.getTime() + TimeUnit.DAYS.toMillis(30));

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
        ReflectionTestUtils.setField(service, "userService", userService);
    }

    /** Mirrors the service's own Date-to-LocalDateTime conversion (system default zone). */
    private static LocalDateTime asLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static User activeUser() {
        User user = new User("someone", "hashed", EMAIL);
        user.setId(1L);
        user.setAutoReactivationEnabled(true);
        return user;
    }

    private static User deletedUser() {
        User user = activeUser();
        user.setDeletedAt(DELETED_AT);
        user.setScheduledDeletionAt(SCHEDULED_DELETION_AT);
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("throws UsernameNotFoundException for an unknown email")
        void throwsForUnknownEmail() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("ghost@example.com");

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("returns an active user as-is without consulting the UserService")
        void returnsActiveUser() {
            User user = activeUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            UserDetails result = service.loadUserByUsername(EMAIL);

            assertThat(result).isSameAs(user);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("auto-reactivates a deleted user within the grace period and returns the reactivated user")
        void reactivatesWithinGracePeriod() {
            User user = deletedUser();
            User reactivated = activeUser();
            reactivated.setId(2L);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userService.isWithinGracePeriod(user)).thenReturn(true);
            when(userService.reactivateUser(user)).thenReturn(reactivated);

            UserDetails result = service.loadUserByUsername(EMAIL);

            assertThat(result)
                    .as("the reactivated instance from UserService is returned, not the stale one")
                    .isSameAs(reactivated);
            verify(userService).reactivateUser(user);
        }

        @Test
        @DisplayName("rejects a deleted user whose auto-reactivation is disabled, without checking the grace period")
        void rejectsWhenAutoReactivationDisabled() {
            User user = deletedUser();
            user.setAutoReactivationEnabled(false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                    .isInstanceOf(AccountDeletionScheduledException.class)
                    .hasMessage("Account has been marked for deletion")
                    .asInstanceOf(InstanceOfAssertFactories.type(AccountDeletionScheduledException.class))
                    .satisfies(ex -> {
                        assertThat(ex.getDeletedAt()).isEqualTo(asLocalDateTime(DELETED_AT));
                        assertThat(ex.getScheduledDeletionAt())
                                .isEqualTo(asLocalDateTime(SCHEDULED_DELETION_AT));
                    });

            // The && chain short-circuits on the disabled flag, so the grace period is never computed.
            verify(userService, never()).isWithinGracePeriod(any());
            verify(userService, never()).reactivateUser(any());
        }

        @Test
        @DisplayName("rejects a deleted user outside the grace period instead of reactivating")
        void rejectsOutsideGracePeriod() {
            User user = deletedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userService.isWithinGracePeriod(user)).thenReturn(false);

            assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                    .isInstanceOf(AccountDeletionScheduledException.class)
                    .hasMessage("Account has been marked for deletion");

            verify(userService, never()).reactivateUser(any());
        }

        @Test
        @DisplayName("rejects a deleted user with AccountDeletionScheduledException when no UserService is wired")
        void rejectsWithoutUserService() {
            // A fresh instance whose optional userService field is deliberately left null,
            // exactly as Spring leaves it when the bean is absent (required = false).
            CustomUserDetailsService detached = new CustomUserDetailsService(userRepository);
            User user = deletedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            // Pins the userService != null guard: this must be the domain exception, never an NPE.
            assertThatThrownBy(() -> detached.loadUserByUsername(EMAIL))
                    .isInstanceOf(AccountDeletionScheduledException.class)
                    .hasMessage("Account has been marked for deletion");

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("survives a deleted user with a null scheduledDeletionAt and carries the null through")
        void toleratesNullScheduledDeletionAt() {
            User user = deletedUser();
            user.setScheduledDeletionAt(null);
            user.setAutoReactivationEnabled(false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            // Regression: the Date-to-LocalDateTime conversion used to dereference the null
            // scheduledDeletionAt and blow up with an NPE before the domain exception was built.
            assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                    .isInstanceOf(AccountDeletionScheduledException.class)
                    .asInstanceOf(InstanceOfAssertFactories.type(AccountDeletionScheduledException.class))
                    .satisfies(ex -> {
                        assertThat(ex.getScheduledDeletionAt()).isNull();
                        assertThat(ex.getDeletedAt()).isEqualTo(asLocalDateTime(DELETED_AT));
                    });
        }
    }
}

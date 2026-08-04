package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.CommentRepository;
import com.jomariabejo.connectly_api.repository.LikeRespository;
import com.jomariabejo.connectly_api.repository.PasswordResetTokenRepository;
import com.jomariabejo.connectly_api.repository.PostRepository;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}, concentrating on the soft-delete lifecycle:
 * delete -> 30-day grace period -> reactivate or permanently delete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final long GRACE_PERIOD_DAYS = 30;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LikeRespository likeRespository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("someone", "secret", "someone@example.com");
        user.setId(1L);
    }

    private static Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    @Nested
    @DisplayName("softDeleteUser")
    class SoftDelete {

        @Test
        @DisplayName("stamps deletedAt, schedules removal 30 days out and clears the active flag")
        void marksUserDeleted() {
            when(userRepository.save(user)).thenReturn(user);

            Date before = new Date();
            User result = userService.softDeleteUser(user);
            Date after = new Date();

            assertThat(result.getDeletedAt()).isBetween(before, after, true, true);
            assertThat(result.isActive()).isFalse();

            // Calendar.add(DAY_OF_MONTH, 30) is calendar arithmetic, so a DST transition inside the
            // window shifts the result by an hour. Assert the day count, tolerating that hour.
            long graceMillis = result.getScheduledDeletionAt().getTime() - result.getDeletedAt().getTime();
            assertThat(TimeUnit.MILLISECONDS.toDays(graceMillis))
                    .as("grace period in days")
                    .isBetween(GRACE_PERIOD_DAYS - 1, GRACE_PERIOD_DAYS);

            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("reactivateUser")
    class Reactivate {

        @Test
        @DisplayName("clears both deletion timestamps and restores the active flag")
        void clearsDeletionState() {
            user.setDeletedAt(new Date());
            user.setScheduledDeletionAt(daysFromNow(30));
            user.setActive(false);
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.reactivateUser(user);

            assertThat(result.getDeletedAt()).isNull();
            assertThat(result.getScheduledDeletionAt()).isNull();
            assertThat(result.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("extendDeletionGracePeriod")
    class ExtendGracePeriod {

        @Test
        @DisplayName("pushes the scheduled deletion out by the requested days")
        void extendsSchedule() {
            Date originallyScheduled = daysFromNow(10);
            user.setDeletedAt(new Date());
            user.setScheduledDeletionAt(originallyScheduled);
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.extendDeletionGracePeriod(user, 5);

            long extensionMillis =
                    result.getScheduledDeletionAt().getTime() - originallyScheduled.getTime();
            assertThat(TimeUnit.MILLISECONDS.toDays(extensionMillis)).isBetween(4L, 5L);
        }

        @Test
        @DisplayName("refuses when the user was never scheduled for deletion")
        void rejectsUnscheduledUser() {
            assertThatThrownBy(() -> userService.extendDeletionGracePeriod(user, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("User is not scheduled for deletion");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isWithinGracePeriod")
    class GracePeriod {

        @Test
        @DisplayName("false for an account that was never deleted")
        void falseForActiveUser() {
            assertThat(userService.isWithinGracePeriod(user)).isFalse();
        }

        @Test
        @DisplayName("true while the scheduled deletion is still in the future")
        void trueBeforeDeadline() {
            user.setDeletedAt(new Date());
            user.setScheduledDeletionAt(daysFromNow(1));

            assertThat(userService.isWithinGracePeriod(user)).isTrue();
        }

        @Test
        @DisplayName("false once the scheduled deletion has passed")
        void falseAfterDeadline() {
            user.setDeletedAt(daysFromNow(-31));
            user.setScheduledDeletionAt(daysFromNow(-1));

            assertThat(userService.isWithinGracePeriod(user)).isFalse();
        }
    }

    @Nested
    @DisplayName("checkAndDeleteScheduledUsers")
    class ScheduledCleanup {

        @Test
        @DisplayName("hard-deletes every due account and reports the count")
        void deletesDueAccounts() {
            User second = new User("other", "secret", "other@example.com");
            second.setId(2L);
            when(userRepository.findUsersScheduledForDeletion()).thenReturn(List.of(user, second));
            when(postRepository.findByCreatedById(anyLong())).thenReturn(List.of());

            assertThat(userService.checkAndDeleteScheduledUsers()).isEqualTo(2);

            verify(userRepository).delete(user);
            verify(userRepository).delete(second);
        }

        @Test
        @DisplayName("clears dependants before the user row, so the delete is not blocked")
        void removesDependantsFirst() {
            Post post = new Post();
            post.setId(10L);
            post.setCreatedBy(user);
            when(postRepository.findByCreatedById(1L)).thenReturn(List.of(post));

            userService.permanentlyDeleteUser(user);

            // Every FK to app_user is NO ACTION, so without this the delete threw a constraint
            // violation, the scheduled task swallowed it, and the account was never purged.
            InOrder order = inOrder(likeRespository, commentRepository, postRepository,
                    tokenRepository, passwordResetTokenRepository, userRepository);
            order.verify(likeRespository).deleteAllByUser(user);
            order.verify(commentRepository).deleteAllByUser(user);
            order.verify(likeRespository).deleteAllByPost(post);
            order.verify(commentRepository).deleteAllByPost(post);
            order.verify(postRepository).delete(post);
            order.verify(tokenRepository).deleteByUser(user);
            order.verify(passwordResetTokenRepository).deleteAllByUser(user);
            order.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("reports zero when nothing is due")
        void handlesEmptyQueue() {
            when(userRepository.findUsersScheduledForDeletion()).thenReturn(List.of());

            assertThat(userService.checkAndDeleteScheduledUsers()).isZero();

            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("confirmRegistration")
    class ConfirmRegistration {

        @Test
        @DisplayName("stores a verification token that expires in 24 hours")
        void storesTokenWith24HourExpiry() {
            userService.confirmRegistration(user, "the-token");

            ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokenRepository).save(saved.capture());

            VerificationToken token = saved.getValue();
            assertThat(token.getToken()).isEqualTo("the-token");
            assertThat(token.getUser()).isEqualTo(user);

            // 24h minus however long the call took, so the truncated hour count is 23 or 24.
            long ttlMillis = token.getExpiryDate().getTime() - System.currentTimeMillis();
            assertThat(TimeUnit.MILLISECONDS.toHours(ttlMillis)).isBetween(23L, 24L);
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("maps a Page into the PaginationDto envelope")
        void mapsPageMetadata() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            PaginationDto<UserResponseDto> result = userService.getAllUsersPaginated(pageable);

            // The page carries the safe projection now, not the entity -- no password hash.
            assertThat(result.getContent()).singleElement()
                    .satisfies(dto -> assertThat(dto.getEmail()).isEqualTo(user.getEmail()));
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("passes every filter field through to the repository query")
        void forwardsFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            UserFilterDto filter = new UserFilterDto("someone", "someone@example.com", "Some", "One");
            when(userRepository.findWithFilters("someone", "someone@example.com", "Some", "One", pageable))
                    .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            PaginationDto<UserResponseDto> result = userService.getAllUsersWithFilters(filter, pageable);

            assertThat(result.getContent()).singleElement()
                    .satisfies(dto -> assertThat(dto.getEmail()).isEqualTo(user.getEmail()));
            verify(userRepository).findWithFilters("someone", "someone@example.com", "Some", "One", pageable);
        }
    }

    @Nested
    @DisplayName("lookups")
    class Lookups {

        @Test
        @DisplayName("getUserById only resolves accounts that are not soft-deleted")
        void resolvesActiveUsersOnly() {
            when(userRepository.findActiveUserById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findActiveUserById(2L)).thenReturn(Optional.empty());

            assertThat(userService.getUserById(1L)).contains(user);
            assertThat(userService.getUserById(2L)).isEmpty();
        }
    }
}

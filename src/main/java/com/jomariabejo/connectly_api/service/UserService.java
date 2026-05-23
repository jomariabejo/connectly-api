package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.config.AccountDeletionConfig;
import com.jomariabejo.connectly_api.dto.AdminDeleteAccountRequestDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.exception.AccountReactivationFailedException;
import com.jomariabejo.connectly_api.exception.InvalidReactivationTokenException;
import com.jomariabejo.connectly_api.exception.UserNotFoundException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final RateLimitingService rateLimitingService;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       RateLimitingService rateLimitingService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimitingService = rateLimitingService;
    }

    public List<User> allUsers() {
        return new ArrayList<>(userRepository.findAll());
    }

    public void confirmRegistration(User user, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(calculateExpiryDate(60 * 24)); // 24 hours
        tokenRepository.save(verificationToken);
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public Page<User> getUsers(UserFilterDto filterDto, Pageable pageable) {
        if (filterDto != null && filterDto.hasAnyFilter()) {
            return userRepository.findWithFilters(
                    filterDto.getUsername(),
                    filterDto.getEmail(),
                    filterDto.getFirstName(),
                    filterDto.getLastName(),
                    pageable
            );
        }

        return userRepository.findAll(pageable);
    }

    @Transactional
    public DeleteAccountResult scheduleAccountDeletion(User user) {
        User deletedUser = softDeleteUser(user);
        VerificationToken token = createReactivationToken(deletedUser);
        return new DeleteAccountResult(deletedUser, token);
    }

    @Transactional
    public VerificationToken createReactivationToken(User user) {
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Date.from(Instant.now().plus(AccountDeletionConfig.GRACE_PERIOD)));
        return tokenRepository.save(token);
    }

    @Transactional
    public User softDeleteUser(User user) {
        Instant now = Instant.now();

        user.setDeletedAt(Date.from(now));
        user.setScheduledDeletionAt(Date.from(now.plus(AccountDeletionConfig.GRACE_PERIOD)));
        user.setActive(false);

        return userRepository.save(user);
    }

    @Transactional
    public void reactivateAccount(String reactivationToken) {
        if (rateLimitingService.isRateLimited(reactivationToken, AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION)) {
            throw new AccountReactivationFailedException("Too many reactivation attempts. Please try again later.");
        }
        rateLimitingService.recordAttempt(reactivationToken, AccountDeletionConfig.REACTIVATION_RATE_LIMIT_ACTION);

        VerificationToken token = tokenRepository.findByToken(reactivationToken)
                .orElseThrow(() -> new InvalidReactivationTokenException("Invalid reactivation token"));

        if (token.getExpiryDate().before(new Date())) {
            throw new InvalidReactivationTokenException("Reactivation token has expired");
        }

        User user = token.getUser();
        if (!isWithinGracePeriod(user)) {
            throw new AccountReactivationFailedException("Grace period has expired. Account cannot be reactivated.");
        }

        reactivateUser(user);
        tokenRepository.delete(token);
    }

    @Transactional
    public User reactivateUser(User user) {
        user.setDeletedAt(null);
        user.setScheduledDeletionAt(null);
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public void adminDeleteUser(Long id, AdminDeleteAccountRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (requestDto != null && requestDto.isForceDelete()) {
            permanentlyDeleteUser(user);
            return;
        }

        if (user.getDeletedAt() == null) {
            softDeleteUser(user);
        }
    }

    @Transactional
    public void permanentlyDeleteUser(User user) {
        userRepository.delete(user);
    }

    @Transactional
    public User extendDeletionGracePeriod(Long id, int additionalDays) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (user.getScheduledDeletionAt() == null) {
            throw new IllegalStateException("User is not scheduled for deletion");
        }

        Instant extendedDeletionAt = user.getScheduledDeletionAt().toInstant()
                .plus(java.time.Duration.ofDays(additionalDays));
        user.setScheduledDeletionAt(Date.from(extendedDeletionAt));
        return userRepository.save(user);
    }

    public int checkAndDeleteScheduledUsers() {
        List<User> scheduledForDeletion = userRepository.findUsersScheduledForDeletion();

        for (User user : scheduledForDeletion) {
            permanentlyDeleteUser(user);
        }

        return scheduledForDeletion.size();
    }

    public boolean isWithinGracePeriod(User user) {
        return user.getDeletedAt() != null
                && user.getScheduledDeletionAt() != null
                && user.getScheduledDeletionAt().after(new Date());
    }

    public User getRequiredUserById(Long id) {
        return userRepository.findActiveUserById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Page<User> getUsersScheduledForDeletion(Pageable pageable) {
        return userRepository.findUsersScheduledForDeletion(pageable);
    }
}

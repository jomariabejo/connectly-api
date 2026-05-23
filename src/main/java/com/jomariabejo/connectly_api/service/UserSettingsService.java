package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.UserSettingsUpdateDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.UserSettings;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.UserSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, UserRepository userRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gets or creates user settings. If settings don't exist for a user, creates default settings.
     *
     * @param user the user
     * @return UserSettings for the user
     */
    public UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUser(user)
            .orElseGet(() -> {
                UserSettings settings = new UserSettings(user);
                return userSettingsRepository.save(settings);
            });
    }

    /**
     * Updates the auto-approve followers setting for a user.
     * When true, follow requests are automatically approved.
     * When false, follow requests remain pending (user has a private account).
     *
     * @param user  the user
     * @param value true to auto-approve, false to require approval
     * @return updated UserSettings
     */
    public UserSettings updateAutoApproveFollowers(User user, boolean value) {
        UserSettings settings = getOrCreateSettings(user);
        settings.setAutoApproveFollowers(value);
        
        // Update user's account privacy setting too
        user.setAccountPrivate(!value);
        
        log.info("Updated auto-approve followers setting for user {} to {}", user.getId(), value);
        return userSettingsRepository.save(settings);
    }

    /**
     * Updates the allow-following setting for a user.
     * When false, other users cannot follow this user.
     *
     * @param user  the user
     * @param value true to allow following, false to block follows
     * @return updated UserSettings
     */
    public UserSettings updateAllowFollowing(User user, boolean value) {
        UserSettings settings = getOrCreateSettings(user);
        settings.setAllowFollowing(value);
        
        log.info("Updated allow following setting for user {} to {}", user.getId(), value);
        return userSettingsRepository.save(settings);
    }

    /**
     * Updates the account privacy setting for a user.
     * This is a convenience method that coordinates with UserSettings.
     *
     * @param user      the user
     * @param isPrivate true for private account (requires follow approval), false for public
     * @return updated User
     */
    public User updateAccountPrivacy(User user, boolean isPrivate) {
        user.setAccountPrivate(isPrivate);
        
        // Update settings to match
        UserSettings settings = getOrCreateSettings(user);
        settings.setAutoApproveFollowers(!isPrivate);
        userSettingsRepository.save(settings);
        
        log.info("Updated account privacy for user {} to private={}", user.getId(), isPrivate);
        return user;
    }

    public UserSettings updateSettings(User user, UserSettingsUpdateDto request) {
        UserSettings settings = getOrCreateSettings(user);

        if (request.getPrivateAccount() != null) {
            user.setAccountPrivate(request.getPrivateAccount());
            settings.setAutoApproveFollowers(!request.getPrivateAccount());
        }

        if (request.getAutoApproveFollowers() != null) {
            settings.setAutoApproveFollowers(request.getAutoApproveFollowers());
            user.setAccountPrivate(!request.getAutoApproveFollowers());
        }

        if (request.getAllowFollowing() != null) {
            settings.setAllowFollowing(request.getAllowFollowing());
        }

        if (request.getAutoReactivationEnabled() != null) {
            user.setAutoReactivationEnabled(request.getAutoReactivationEnabled());
        }

        userRepository.save(user);
        return userSettingsRepository.save(settings);
    }
}

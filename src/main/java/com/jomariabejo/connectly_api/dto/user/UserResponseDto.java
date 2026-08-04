package com.jomariabejo.connectly_api.dto.user;

import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The safe public projection of a {@link User}.
 *
 * <p>Exists because returning the entity directly leaks the BCrypt password hash and the
 * verification token -- the latter lets anyone who sees the response activate the account without
 * access to the mailbox. Every endpoint that used to serialize {@code User} returns this instead.
 *
 * <p>Deliberately omitted: {@code password}, {@code verificationToken}, {@code expiryDate}.
 */
@Getter
@Setter
@Schema(description = "A user profile. Never carries credentials or verification tokens.")
public class UserResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "helloworld")
    private String username;

    @Schema(example = "test@gmail.com")
    private String email;

    @Schema(example = "Jomari")
    private String firstName;

    @Schema(example = "Abejo")
    private String lastName;

    @Schema(description = "False until the email address has been verified", example = "true")
    private boolean enabled;

    @Schema(description = "Granted role names, without the ROLE_ prefix", example = "[\"USER\"]")
    private Set<String> roles;

    @Schema(description = "Null unless the account is scheduled for deletion")
    private Date deletedAt;

    @Schema(description = "When a scheduled account is permanently removed; null otherwise")
    private Date scheduledDeletionAt;

    @Schema(description = "Whether logging in during the grace period restores the account")
    private boolean autoReactivationEnabled;

    private Date createdAt;

    private Date updatedAt;

    public UserResponseDto() {
    }

    public UserResponseDto(User user) {
        this.id = user.getId();
        // getUsername() is overridden to return the email for Spring Security, so read the field.
        this.username = user.getUsernameField();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.enabled = user.isEnabled();
        this.roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        this.deletedAt = user.getDeletedAt();
        this.scheduledDeletionAt = user.getScheduledDeletionAt();
        this.autoReactivationEnabled = user.isAutoReactivationEnabled();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public static UserResponseDto from(User user) {
        return new UserResponseDto(user);
    }
}

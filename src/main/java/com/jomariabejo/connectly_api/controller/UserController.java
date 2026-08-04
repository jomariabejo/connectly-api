package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.*;
import com.jomariabejo.connectly_api.exception.AccountDeletionScheduledException;
import com.jomariabejo.connectly_api.exception.AccountReactivationFailedException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequestMapping("/users")
@RestController
@Tag(name = "Users", description = "Profile access, account deletion with a 30-day grace period, reactivation, and admin account management.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final VerificationTokenRepository verificationTokenRepository;

    public UserController(UserService userService, 
                         AuthenticationService authenticationService,
                         VerificationTokenRepository verificationTokenRepository) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Operation(summary = "Get the caller's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    })
    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        User currentUser = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(currentUser);
    }

    @Operation(
            summary = "List all users",
            description = "Unpaginated. Note the trailing slash: the path is `/users/`, not `/users`. "
                    + "Prefer /users/paginated.")
    @ApiResponse(responseCode = "200", description = "Users returned")
    @GetMapping("/")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();

        return ResponseEntity.ok(users);
    }

    // Pagination endpoints
    @Operation(
            summary = "List users (paginated, filterable)",
            description = "Defaults to `?page=0&size=10&sort=id,asc`. Passing any of `username`, `email`, `firstName` "
                    + "or `lastName` switches to the filtered query.")
    @ApiResponse(responseCode = "200", description = "A PaginationDto page of users")
    @GetMapping("/paginated")
    public ResponseEntity<PaginationDto<User>> getAllUsersPaginated(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {

        UserFilterDto filterDto = new UserFilterDto(username, email, firstName, lastName);
        PaginationDto<User> result;

        if (hasFilters(filterDto)) {
            result = userService.getAllUsersWithFilters(filterDto, pageable);
        } else {
            result = userService.getAllUsersPaginated(pageable);
        }

        return ResponseEntity.ok(result);
    }

    private boolean hasFilters(UserFilterDto filterDto) {
        return filterDto.getUsername() != null || filterDto.getEmail() != null ||
               filterDto.getFirstName() != null || filterDto.getLastName() != null;
    }

    /**
     * Delete user account (soft-delete with 30-day grace period).
     * User data is scheduled for permanent deletion after 30 days.
     */
    @Operation(
            summary = "Delete the caller's account (soft delete)",
            description = "Marks the account deleted and schedules permanent removal 30 days out, then mails a "
                    + "reactivation token. Returns 202 Accepted, not 204 -- deletion is scheduled, not immediate. "
                    + "The body carries both timestamps and the grace period in days.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Deletion scheduled; reactivation token issued"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    })
    @DeleteMapping("/me")
    public ResponseEntity<DeleteAccountResponseDto> deleteAccount(
            @RequestBody(required = false) DeleteAccountRequestDto requestDto) {
        
        User currentUser = authenticationService.getAuthenticatedUser();
        
        // Apply default auto-reactivation setting
        if (requestDto != null) {
            currentUser.setAutoReactivationEnabled(requestDto.isAutoReactivationEnabled());
        }
        
        // Soft-delete the user
        User deletedUser = userService.softDeleteUser(currentUser);
        
        // Create a reactivation token
        String reactivationToken = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setUser(deletedUser);
        token.setToken(reactivationToken);
        token.setExpiryDate(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000)); // 30 days
        verificationTokenRepository.save(token);
        
        // Convert timestamps to LocalDateTime
        LocalDateTime deletedAt = Instant.ofEpochMilli(deletedUser.getDeletedAt().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime scheduledDeletionAt = Instant.ofEpochMilli(deletedUser.getScheduledDeletionAt().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        
        DeleteAccountResponseDto response = new DeleteAccountResponseDto(
                "Account has been marked for deletion",
                deletedAt,
                scheduledDeletionAt,
                30,
                deletedUser.isAutoReactivationEnabled(),
                "Your account will be permanently deleted on: " + scheduledDeletionAt + 
                ". If auto-reactivation is enabled, you can restore your account by logging in. " +
                "Otherwise, visit the reactivation link sent to your email."
        );
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Reactivate a deleted user account within the grace period.
     * Requires verification token sent to user's email.
     */
    @Operation(
            summary = "Reactivate a deleted account",
            description = "Restores an account inside its 30-day grace period using the reactivation token from the "
                    + "deletion email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account reactivated"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reactivation token, or the grace period has lapsed")
    })
    @PostMapping("/reactivate")
    public ResponseEntity<String> reactivateAccount(
            @RequestBody ReactivateAccountRequestDto requestDto) {
        
        Optional<VerificationToken> tokenOpt =
        verificationTokenRepository.findByToken(requestDto.getReactivationToken());

        if (tokenOpt.isEmpty()) {
            throw new AccountReactivationFailedException("Invalid or expired reactivation token");
        }
        
        VerificationToken token = tokenOpt.get();
        User user = token.getUser();
        
        // Check if token has expired
        if (token.getExpiryDate().before(new Date())) {
            throw new AccountReactivationFailedException("Reactivation token has expired");
        }
        
        // Check if user is within grace period
        if (!userService.isWithinGracePeriod(user)) {
            throw new AccountReactivationFailedException("Grace period has expired. Account cannot be reactivated.");
        }
        
        // Reactivate user
        userService.reactivateUser(user);
        
        // Delete the verification token
        verificationTokenRepository.delete(token);
        
        return ResponseEntity.ok("Account successfully reactivated");
    }

    /**
     * Admin endpoint: Permanently delete a user account (force delete, no grace period).
     * Can only be called by users with ADMIN role.
     */
    @Operation(
            summary = "[ADMIN] Delete a user account",
            description = "Soft-deletes by default. Send `{\"forceDelete\": true}` to bypass the grace period and "
                    + "remove the account immediately. Full path: `/users/admin/users/{id}`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted or marked for deletion"),
            @ApiResponse(responseCode = "403", description = "Caller does not hold ROLE_ADMIN"),
            @ApiResponse(responseCode = "404", description = "No user exists with this id")
    })
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminDeleteUser(
            @PathVariable Long id,
            @RequestBody(required = false) AdminDeleteAccountRequestDto requestDto) {
        
        Optional<User> userOpt = userService.getUserById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        
        User user = userOpt.get();
        
        if (requestDto != null && requestDto.isForceDelete()) {
            // Force permanent deletion (bypass grace period)
            userService.permanentlyDeleteUser(user);
            return ResponseEntity.ok("User permanently deleted");
        } else {
            // Soft-delete if not already deleted
            if (user.getDeletedAt() == null) {
                userService.softDeleteUser(user);
            }
            return ResponseEntity.ok("User account marked for deletion");
        }
    }

    /**
     * Admin endpoint: Extend the deletion grace period for a user.
     * Can only be called by users with ADMIN role.
     */
    @Operation(
            summary = "[ADMIN] Extend a deletion grace period",
            description = "Pushes the scheduled deletion out by `extensionDays`. Full path: "
                    + "`/users/admin/users/{id}/extend-deletion`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grace period extended"),
            @ApiResponse(responseCode = "400", description = "extensionDays missing or <= 0, or the user is not scheduled for deletion"),
            @ApiResponse(responseCode = "403", description = "Caller does not hold ROLE_ADMIN"),
            @ApiResponse(responseCode = "404", description = "No user exists with this id")
    })
    @PutMapping("/admin/users/{id}/extend-deletion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> extendDeletionPeriod(
            @PathVariable Long id,
            @RequestBody AdminDeleteAccountRequestDto requestDto) {
        
        if (requestDto.getExtensionDays() == null || requestDto.getExtensionDays() <= 0) {
            return ResponseEntity.badRequest().body("Extension days must be greater than 0");
        }
        
        Optional<User> userOpt = userService.getUserById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        
        User user = userOpt.get();
        
        if (user.getDeletedAt() == null) {
            return ResponseEntity.badRequest().body("User is not scheduled for deletion");
        }
        
        userService.extendDeletionGracePeriod(user, requestDto.getExtensionDays());
        
        return ResponseEntity.ok("Deletion grace period extended by " + requestDto.getExtensionDays() + " days");
    }

    /**
     * Admin endpoint: List all users scheduled for permanent deletion.
     * Can only be called by users with ADMIN role.
     */
    @Operation(
            summary = "[ADMIN] List accounts pending permanent deletion",
            description = "Full path: `/users/admin/users/scheduled-deletion`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts awaiting permanent deletion"),
            @ApiResponse(responseCode = "403", description = "Caller does not hold ROLE_ADMIN")
    })
    @GetMapping("/admin/users/scheduled-deletion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersScheduledForDeletion() {
        List<User> scheduledUsers = userService.getUsersScheduledForDeletion();
        return ResponseEntity.ok(scheduledUsers);
    }
}
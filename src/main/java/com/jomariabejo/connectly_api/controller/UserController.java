package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.DeleteAccountRequestDto;
import com.jomariabejo.connectly_api.dto.DeleteAccountResponseDto;
import com.jomariabejo.connectly_api.dto.ReactivateAccountRequestDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.dto.UserSettingsUpdateDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.dto.user.UserSettingsResponseDto;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.UserSettings;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.DeleteAccountResult;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequestMapping("/v1/users")
@RestController
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserSettingsService userSettingsService;
    private final UserMapper userMapper;

    public UserController(UserService userService,
                          AuthenticationService authenticationService,
                          UserSettingsService userSettingsService,
                          UserMapper userMapper) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.userSettingsService = userSettingsService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "Get authenticated user profile")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> authenticatedUser() {
        User currentUser = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(userMapper.toResponseDto(currentUser));
    }

    @Operation(summary = "List users with optional filters and pagination")
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getUsers(
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @Valid UserFilterDto filters) {
        return ResponseEntity.ok(userMapper.toResponseDto(userService.getUsers(filters, pageable)));
    }

    @Operation(summary = "Schedule authenticated user account deletion")
    @DeleteMapping("/me")
    public ResponseEntity<DeleteAccountResponseDto> deleteAccount(
            @Valid @RequestBody(required = false) DeleteAccountRequestDto requestDto) {
        User currentUser = authenticationService.getAuthenticatedUser();
        DeleteAccountResult result = userService.scheduleAccountDeletion(currentUser);

        DeleteAccountResponseDto response = new DeleteAccountResponseDto(
                "Account has been marked for deletion",
                result.getDeletedAt(),
                result.getScheduledDeletionAt(),
                result.getGracePeriodDays(),
                currentUser.isAutoReactivationEnabled(),
                "Your account will be permanently deleted on: " + result.getScheduledDeletionAt() +
                        ". Use the reactivation link sent to your email to restore your account during the grace period."
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reactivate a deleted user account")
    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivateAccount(
            @Valid @RequestBody ReactivateAccountRequestDto requestDto) {
        userService.reactivateAccount(requestDto.getReactivationToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get authenticated user settings")
    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsResponseDto> getUserSettings() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        UserSettings settings = userSettingsService.getOrCreateSettings(authenticatedUser);
        return ResponseEntity.ok(UserSettingsResponseDto.from(settings));
    }

    @Operation(summary = "Partially update authenticated user settings")
    @PatchMapping("/me/settings")
    public ResponseEntity<UserSettingsResponseDto> updateSettings(
            @Valid @RequestBody UserSettingsUpdateDto requestDto) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        UserSettings settings = userSettingsService.updateSettings(authenticatedUser, requestDto);
        return ResponseEntity.ok(UserSettingsResponseDto.from(settings));
    }
}

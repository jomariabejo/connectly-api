package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.AdminDeleteAccountRequestDto;
import com.jomariabejo.connectly_api.dto.ExtensionRequestDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "List users scheduled for deletion")
    @GetMapping("/scheduled-for-deletion")
    public ResponseEntity<Page<UserResponseDto>> getScheduledDeletions(
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userMapper.toResponseDto(userService.getUsersScheduledForDeletion(pageable)));
    }

    @Operation(summary = "Delete or permanently delete a user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminDeleteAccountRequestDto requestDto) {
        userService.adminDeleteUser(id, requestDto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Extend a user's deletion grace period")
    @PatchMapping("/{id}/deletion-grace-period")
    public ResponseEntity<Void> extendGracePeriod(
            @PathVariable Long id,
            @Valid @RequestBody ExtensionRequestDto requestDto) {
        userService.extendDeletionGracePeriod(id, requestDto.getExtensionDays());
        return ResponseEntity.noContent().build();
    }
}

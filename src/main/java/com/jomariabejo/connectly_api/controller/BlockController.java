package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.model.Block;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.BlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/blocks")
public class BlockController {

    private final BlockService blockService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public BlockController(BlockService blockService, UserRepository userRepository, AuthenticationService authenticationService) {
        this.blockService = blockService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * Block a user
     * POST /v1/blocks/{userId}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            blockService.blockUser(authenticatedUser, targetUser);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Unblock a user
     * DELETE /v1/blocks/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            blockService.unblockUser(authenticatedUser, targetUser);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all users blocked by authenticated user
     * GET /v1/blocks/blocked-users
     */
    @GetMapping("/blocked-users")
    public ResponseEntity<List<User>> getBlockedUsers() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        List<User> blockedUsers = blockService.getBlockedUsers(authenticatedUser);
        return ResponseEntity.ok(blockedUsers);
    }

    /**
     * Get all users who have blocked the authenticated user
     * GET /v1/blocks/blockers
     */
    @GetMapping("/blockers")
    public ResponseEntity<List<User>> getBlockers() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        List<User> blockers = blockService.getBlockers(authenticatedUser);
        return ResponseEntity.ok(blockers);
    }

    /**
     * Check if authenticated user has blocked a target user
     * GET /v1/blocks/{userId}/is-blocked
     */
    @GetMapping("/{userId}/is-blocked")
    public ResponseEntity<Boolean> isBlocked(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean blocked = blockService.isBlocked(authenticatedUser, targetUser);
        return ResponseEntity.ok(blocked);
    }

    /**
     * Check if two users are bidirectionally blocked
     * GET /v1/blocks/{userId}/is-bidirectionally-blocked
     */
    @GetMapping("/{userId}/is-bidirectionally-blocked")
    public ResponseEntity<Boolean> isBidirectionallyBlocked(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean blocked = blockService.isBidirectionallyBlocked(authenticatedUser, targetUser);
        return ResponseEntity.ok(blocked);
    }

    /**
     * Get count of users blocked by authenticated user
     * GET /v1/blocks/count/blocked
     */
    @GetMapping("/count/blocked")
    public ResponseEntity<Long> getBlockedCount() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        long count = blockService.getBlockedCount(authenticatedUser);
        return ResponseEntity.ok(count);
    }

    /**
     * Get count of users who have blocked the authenticated user
     * GET /v1/blocks/count/blockers
     */
    @GetMapping("/count/blockers")
    public ResponseEntity<Long> getBlockerCount() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        long count = blockService.getBlockerCount(authenticatedUser);
        return ResponseEntity.ok(count);
    }
}

package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.FollowResponseDto;
import com.jomariabejo.connectly_api.model.Follow;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class FollowController {

    private final FollowService followService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public FollowController(FollowService followService, UserRepository userRepository, AuthenticationService authenticationService) {
        this.followService = followService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * Follow a user
     * POST /api/users/{userId}/follow
     */
    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<FollowResponseDto> followUser(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Follow follow = followService.followUser(authenticatedUser, targetUser);
            FollowResponseDto response = new FollowResponseDto(
                    follow.getId(),
                    follow.getFollower().getId(),
                    follow.getFollowing().getId(),
                    follow.getApproved(),
                    follow.getRequestStatus().toString()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Unfollow a user
     * DELETE /api/users/{userId}/follow
     */
    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<Void> unfollowUser(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            followService.unfollowUser(authenticatedUser, targetUser);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get followers of a user
     * GET /api/users/{userId}/followers?page=0&size=10
     */
    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<Page<FollowResponseDto>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followers = followService.getFollowers(targetUser, pageable);

        Page<FollowResponseDto> response = followers.map(follow -> 
            new FollowResponseDto(
                    follow.getId(),
                    follow.getFollower().getId(),
                    follow.getFollowing().getId(),
                    follow.getApproved(),
                    follow.getRequestStatus().toString()
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get users that a user is following
     * GET /api/users/{userId}/following?page=0&size=10
     */
    @GetMapping("/users/{userId}/following")
    public ResponseEntity<Page<FollowResponseDto>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> following = followService.getFollowing(targetUser, pageable);

        Page<FollowResponseDto> response = following.map(follow -> 
            new FollowResponseDto(
                    follow.getId(),
                    follow.getFollower().getId(),
                    follow.getFollowing().getId(),
                    follow.getApproved(),
                    follow.getRequestStatus().toString()
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get pending follow requests for the authenticated user
     * GET /api/follow-requests?page=0&size=10
     */
    @GetMapping("/follow-requests")
    public ResponseEntity<Page<FollowResponseDto>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> pendingRequests = followService.getPendingRequests(authenticatedUser, pageable);

        Page<FollowResponseDto> response = pendingRequests.map(follow -> 
            new FollowResponseDto(
                    follow.getId(),
                    follow.getFollower().getId(),
                    follow.getFollowing().getId(),
                    follow.getApproved(),
                    follow.getRequestStatus().toString()
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Approve a follow request
     * POST /api/follow-requests/{followId}/approve
     */
    @PostMapping("/follow-requests/{followId}/approve")
    public ResponseEntity<FollowResponseDto> approveFollowRequest(@PathVariable Long followId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        // Find the follow relationship
        Follow followRequest = followService.getPendingRequests(authenticatedUser, PageRequest.of(0, 1)).getContent()
                .stream()
                .filter(f -> f.getId().equals(followId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        try {
            Follow approved = followService.approveFollowRequest(authenticatedUser, followRequest.getFollower());
            FollowResponseDto response = new FollowResponseDto(
                    approved.getId(),
                    approved.getFollower().getId(),
                    approved.getFollowing().getId(),
                    approved.getApproved(),
                    approved.getRequestStatus().toString()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reject a follow request
     * POST /api/follow-requests/{followId}/reject
     */
    @PostMapping("/follow-requests/{followId}/reject")
    public ResponseEntity<Void> rejectFollowRequest(@PathVariable Long followId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        // Find the follow relationship
        Follow followRequest = followService.getPendingRequests(authenticatedUser, PageRequest.of(0, 1)).getContent()
                .stream()
                .filter(f -> f.getId().equals(followId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        try {
            followService.rejectFollowRequest(authenticatedUser, followRequest.getFollower());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get follower count for a user
     * GET /api/users/{userId}/follower-count
     */
    @GetMapping("/users/{userId}/follower-count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long count = followService.getFollowerCount(targetUser);
        return ResponseEntity.ok(count);
    }

    /**
     * Get following count for a user
     * GET /api/users/{userId}/following-count
     */
    @GetMapping("/users/{userId}/following-count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long count = followService.getFollowingCount(targetUser);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if authenticated user is following a target user
     * GET /api/users/{userId}/is-following
     */
    @GetMapping("/users/{userId}/is-following")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean following = followService.isFollowing(authenticatedUser, targetUser);
        return ResponseEntity.ok(following);
    }
}

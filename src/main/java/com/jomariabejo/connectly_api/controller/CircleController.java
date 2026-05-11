package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.CircleDto;
import com.jomariabejo.connectly_api.model.Circle;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.CircleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/circles")
public class CircleController {

    private final CircleService circleService;
    private final AuthenticationService authenticationService;

    public CircleController(CircleService circleService, AuthenticationService authenticationService) {
        this.circleService = circleService;
        this.authenticationService = authenticationService;
    }

    /**
     * Create a new circle
     * POST /v1/circles
     */
    @PostMapping
    public ResponseEntity<CircleDto> createCircle(@RequestBody CircleDto request) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            Circle circle = circleService.createCircle(authenticatedUser, request.getName(), request.getDescription());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(circle));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all circles owned by authenticated user
     * GET /v1/circles
     */
    @GetMapping
    public ResponseEntity<List<CircleDto>> getCircles() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        List<Circle> circles = circleService.getCircles(authenticatedUser);
        List<CircleDto> dtos = circles.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific circle
     * GET /v1/circles/{circleId}
     */
    @GetMapping("/{circleId}")
    public ResponseEntity<CircleDto> getCircle(@PathVariable Long circleId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            Circle circle = circleService.getCircle(authenticatedUser, circleId);
            return ResponseEntity.ok(mapToDto(circle));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a circle's name and description
     * PUT /v1/circles/{circleId}
     */
    @PutMapping("/{circleId}")
    public ResponseEntity<CircleDto> updateCircle(@PathVariable Long circleId, @RequestBody CircleDto request) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            circleService.updateCircle(authenticatedUser, circleId, request.getName(), request.getDescription());
            Circle updated = circleService.getCircle(authenticatedUser, circleId);
            return ResponseEntity.ok(mapToDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a circle
     * DELETE /v1/circles/{circleId}
     */
    @DeleteMapping("/{circleId}")
    public ResponseEntity<Void> deleteCircle(@PathVariable Long circleId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            circleService.deleteCircle(authenticatedUser, circleId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add a member to a circle
     * POST /v1/circles/{circleId}/members/{memberId}
     */
    @PostMapping("/{circleId}/members/{memberId}")
    public ResponseEntity<Void> addMember(@PathVariable Long circleId, @PathVariable Long memberId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            circleService.addMemberToCircle(authenticatedUser, circleId, memberId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a member from a circle
     * DELETE /v1/circles/{circleId}/members/{memberId}
     */
    @DeleteMapping("/{circleId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long circleId, @PathVariable Long memberId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            circleService.removeMemberFromCircle(authenticatedUser, circleId, memberId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get member count for a circle
     * GET /v1/circles/{circleId}/member-count
     */
    @GetMapping("/{circleId}/member-count")
    public ResponseEntity<Long> getMemberCount(@PathVariable Long circleId) {
        User authenticatedUser = authenticationService.getAuthenticatedUser();

        try {
            // Verify user owns the circle
            circleService.getCircle(authenticatedUser, circleId);
            long count = circleService.getMemberCount(circleId);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all circles that authenticated user is a member of
     * GET /v1/circles/member-of
     */
    @GetMapping("/member-of")
    public ResponseEntity<List<CircleDto>> getCirclesMemberOf() {
        User authenticatedUser = authenticationService.getAuthenticatedUser();
        List<Circle> circles = circleService.getCirclesMemberOf(authenticatedUser);
        List<CircleDto> dtos = circles.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private CircleDto mapToDto(Circle circle) {
        CircleDto dto = new CircleDto();
        dto.setId(circle.getId());
        dto.setName(circle.getName());
        dto.setDescription(circle.getDescription());
        dto.setOwnerId(circle.getOwner().getId());
        dto.setMemberCount(circle.getMembers() != null ? circle.getMembers().size() : 0);
        dto.setCreatedAt(circle.getCreatedAt());
        return dto;
    }
}

package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.Circle;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.CircleRepository;
import com.jomariabejo.connectly_api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CircleService manages user circles for custom privacy groups.
 * Circles allow users to create named groups (family, close-friends, work, etc.)
 * and share posts with specific circles using the CIRCLES privacy level.
 */
@Slf4j
@Service
@Transactional
public class CircleService {

    private final CircleRepository circleRepository;
    private final UserRepository userRepository;

    public CircleService(CircleRepository circleRepository, UserRepository userRepository) {
        this.circleRepository = circleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new circle for a user.
     *
     * @param owner  the user creating the circle
     * @param name   the circle name
     * @param description optional circle description
     * @return the created Circle
     * @throws IllegalArgumentException if circle name already exists for user
     */
    public Circle createCircle(User owner, String name, String description) {
        Optional<Circle> existingCircle = circleRepository.findByOwnerAndName(owner, name);
        if (existingCircle.isPresent()) {
            throw new IllegalArgumentException("Circle with this name already exists");
        }

        Circle circle = new Circle(owner, name, description);
        log.info("Created circle '{}' for user {}", name, owner.getId());
        return circleRepository.save(circle);
    }

    /**
     * Creates a new circle with just a name.
     *
     * @param owner the user creating the circle
     * @param name  the circle name
     * @return the created Circle
     */
    public Circle createCircle(User owner, String name) {
        return createCircle(owner, name, null);
    }

    /**
     * Gets all circles owned by a user.
     *
     * @param owner the user
     * @return list of circles owned by the user
     */
    public List<Circle> getCircles(User owner) {
        return circleRepository.findByOwner(owner);
    }

    /**
     * Gets a specific circle owned by a user.
     *
     * @param owner    the user
     * @param circleId the circle ID
     * @return the Circle if found
     * @throws IllegalArgumentException if circle not found or not owned by user
     */
    public Circle getCircle(User owner, Long circleId) {
        return circleRepository.findByOwnerAndId(owner, circleId)
            .orElseThrow(() -> new IllegalArgumentException("Circle not found or not owned by user"));
    }

    /**
     * Adds a member to a circle.
     *
     * @param owner    the circle owner
     * @param circleId the circle ID
     * @param memberId the user to add
     * @throws IllegalArgumentException if circle not found, not owned by user, or member doesn't exist
     */
    public void addMemberToCircle(User owner, Long circleId, Long memberId) {
        Circle circle = getCircle(owner, circleId);

        User member = userRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (circle.getMembers().contains(member)) {
            throw new IllegalArgumentException("User already in circle");
        }

        circle.getMembers().add(member);
        circle.setUpdatedAt(LocalDateTime.now());
        log.info("Added user {} to circle '{}' for owner {}", memberId, circle.getName(), owner.getId());
        circleRepository.save(circle);
    }

    /**
     * Removes a member from a circle.
     *
     * @param owner    the circle owner
     * @param circleId the circle ID
     * @param memberId the user to remove
     * @throws IllegalArgumentException if circle not found or not owned by user
     */
    public void removeMemberFromCircle(User owner, Long circleId, Long memberId) {
        Circle circle = getCircle(owner, circleId);

        User member = userRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (!circle.getMembers().contains(member)) {
            throw new IllegalArgumentException("User not in circle");
        }

        circle.getMembers().remove(member);
        circle.setUpdatedAt(LocalDateTime.now());
        log.info("Removed user {} from circle '{}' for owner {}", memberId, circle.getName(), owner.getId());
        circleRepository.save(circle);
    }

    /**
     * Updates a circle's name and description.
     *
     * @param owner       the circle owner
     * @param circleId    the circle ID
     * @param newName     new name (or null to keep current)
     * @param newDescription new description (or null to keep current)
     * @throws IllegalArgumentException if circle not found or not owned by user
     */
    public void updateCircle(User owner, Long circleId, String newName, String newDescription) {
        Circle circle = getCircle(owner, circleId);

        if (newName != null && !newName.equals(circle.getName())) {
            // Check if new name is unique
            Optional<Circle> existing = circleRepository.findByOwnerAndName(owner, newName);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Circle name already in use");
            }
            circle.setName(newName);
        }

        if (newDescription != null) {
            circle.setDescription(newDescription);
        }

        circle.setUpdatedAt(LocalDateTime.now());
        log.info("Updated circle {} for owner {}", circleId, owner.getId());
        circleRepository.save(circle);
    }

    /**
     * Deletes a circle.
     *
     * @param owner    the circle owner
     * @param circleId the circle ID
     * @throws IllegalArgumentException if circle not found or not owned by user
     */
    public void deleteCircle(User owner, Long circleId) {
        Circle circle = getCircle(owner, circleId);
        log.info("Deleted circle '{}' for owner {}", circle.getName(), owner.getId());
        circleRepository.delete(circle);
    }

    /**
     * Checks if a user is a member of a circle.
     *
     * @param circleId the circle ID
     * @param user     the user
     * @return true if user is a member of the circle
     */
    public boolean isMemberOfCircle(Long circleId, User user) {
        return circleRepository.isMemberOfCircle(circleId, user);
    }

    /**
     * Gets all circles that a user is a member of (from other users' circles).
     *
     * @param user the user
     * @return list of circles the user is a member of
     */
    public List<Circle> getCirclesMemberOf(User user) {
        return circleRepository.findCirclesContainingMember(user);
    }

    /**
     * Gets member count for a circle.
     *
     * @param circleId the circle ID
     * @return count of members
     */
    public long getMemberCount(Long circleId) {
        return circleRepository.countMembers(circleId);
    }
}

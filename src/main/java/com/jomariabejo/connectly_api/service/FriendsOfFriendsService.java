package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.FollowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FriendsOfFriendsService handles friend-of-friend relationship detection and caching.
 * A user is a friend-of-friend if:
 * - User A follows User B (B is A's friend)
 * - User B follows User C (C is B's friend)
 * - Therefore C is A's friend-of-friend
 *
 * This uses caching at 5-minute TTL to avoid expensive graph traversals on every check.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FriendsOfFriendsService {

    private final FollowRepository followRepository;

    public FriendsOfFriendsService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    /**
     * Checks if userA can see userB via friend-of-friend relationship.
     * This is cached to avoid expensive lookups on every privacy check.
     *
     * Relationship: userA → (follows) → userX → (follows) → userB
     * Result: userA and userB are friends-of-friends, so userA can see userB's FRIENDS_OF_FRIENDS posts
     *
     * @param userA the user checking visibility
     * @param userB the user whose post is being viewed
     * @return true if userA is a friend-of-friend of userB (one-way direction)
     */
    @Cacheable(value = "friendsOfFriends", key = "#userA.id + '-' + #userB.id", unless = "#result == false")
    public boolean isFriendOfFriend(User userA, User userB) {
        if (userA == null || userB == null || userA.getId().equals(userB.getId())) {
            return false;
        }

        // Get all direct followers of userB (people B follows)
        List<Long> userBFollowing = getFollowingIds(userB.getId());

        if (userBFollowing.isEmpty()) {
            log.debug("User {} has no followers, cannot be friend-of-friend", userB.getId());
            return false;
        }

        // Get all people that userA follows
        List<Long> userAFollowing = getFollowingIds(userA.getId());

        if (userAFollowing.isEmpty()) {
            log.debug("User {} follows no one, cannot have friend-of-friend", userA.getId());
            return false;
        }

        // Check if there's any intersection: does userA follow anyone who also follows userB?
        boolean isFof = userAFollowing.stream()
            .anyMatch(userBFollowing::contains);

        if (isFof) {
            log.debug("User {} is friend-of-friend with user {} via mutual connection", userA.getId(), userB.getId());
        }

        return isFof;
    }

    /**
     * Gets all users that a user is following (IDs only).
     * Used for friend-of-friend graph traversal.
     *
     * @param userId the user
     * @return list of user IDs this user follows
     */
    private List<Long> getFollowingIds(Long userId) {
        // Create a temporary User object for query (we only need the ID)
        User user = new User();
        user.setId(userId);

        // Get all approved follows
        return followRepository.findApprovedFollowingByUser(user)
            .stream()
            .map(follow -> follow.getFollowing().getId())
            .collect(Collectors.toList());
    }

    /**
     * Gets all followers of a user (IDs only).
     * Used for friend-of-friend graph traversal.
     *
     * @param userId the user
     * @return list of user IDs who follow this user
     */
    private List<Long> getFollowerIds(Long userId) {
        // Create a temporary User object for query (we only need the ID)
        User user = new User();
        user.setId(userId);

        // Get all approved followers
        return followRepository.findApprovedFollowersByUser(user)
            .stream()
            .map(follow -> follow.getFollower().getId())
            .collect(Collectors.toList());
    }

    /**
     * Finds common followers between two users.
     * Useful for recommendations and mutual friend detection.
     *
     * @param userA first user
     * @param userB second user
     * @return set of user IDs who follow both userA and userB
     */
    public Set<Long> findCommonFollowers(User userA, User userB) {
        List<Long> followersA = getFollowerIds(userA.getId());
        List<Long> followersB = getFollowerIds(userB.getId());

        return followersA.stream()
            .filter(followersB::contains)
            .collect(Collectors.toSet());
    }

    /**
     * Finds common people both users follow.
     * Useful for recommendations.
     *
     * @param userA first user
     * @param userB second user
     * @return set of user IDs both users follow
     */
    public Set<Long> findCommonFollowing(User userA, User userB) {
        List<Long> followingA = getFollowingIds(userA.getId());
        List<Long> followingB = getFollowingIds(userB.getId());

        return followingA.stream()
            .filter(followingB::contains)
            .collect(Collectors.toSet());
    }
}

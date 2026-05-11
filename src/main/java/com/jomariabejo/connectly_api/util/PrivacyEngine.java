package com.jomariabejo.connectly_api.util;

import com.jomariabejo.connectly_api.model.PrivacyLevel;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.BlockRepository;
import com.jomariabejo.connectly_api.repository.CircleRepository;
import com.jomariabejo.connectly_api.repository.FollowRepository;
import com.jomariabejo.connectly_api.service.FriendsOfFriendsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PrivacyEngine is a centralized utility for enforcing privacy policies across the application.
 * It provides methods to check if a user can view, comment on, or interact with posts based on privacy levels.
 * This is the single source of truth for access decisions.
 */
@Slf4j
@Component
public class PrivacyEngine {

    private final FollowRepository followRepository;
    private final FriendsOfFriendsService friendsOfFriendsService;
    private final BlockRepository blockRepository;
    private final CircleRepository circleRepository;

    public PrivacyEngine(FollowRepository followRepository, 
                        FriendsOfFriendsService friendsOfFriendsService,
                        BlockRepository blockRepository,
                        CircleRepository circleRepository) {
        this.followRepository = followRepository;
        this.friendsOfFriendsService = friendsOfFriendsService;
        this.blockRepository = blockRepository;
        this.circleRepository = circleRepository;
    }

    /**
     * Determines if a viewer can see a specific post based on privacy level and follow relationships.
     *
     * @param viewer the user attempting to view the post
     * @param post   the post being viewed
     * @return true if viewer can see the post, false otherwise
     */
    public boolean canViewPost(User viewer, Post post) {
        if (viewer == null || post == null) {
            return false;
        }

        // Post owner can always see their own post
        if (viewer.getId().equals(post.getCreatedBy().getId())) {
            return true;
        }

        // Check if viewer is blocked by post owner or has blocked post owner (bidirectional)
        if (blockRepository.isBidirectionallyBlocked(viewer, post.getCreatedBy())) {
            return false;
        }

        PrivacyLevel privacy = post.getPrivacy();

        return switch (privacy) {
            case PUBLIC -> true;
            case FOLLOWERS_ONLY -> isFollowing(viewer, post.getCreatedBy());
            case MUTUALS_ONLY -> isMutual(viewer, post.getCreatedBy());
            case FRIENDS_OF_FRIENDS -> friendsOfFriendsService.isFriendOfFriend(viewer, post.getCreatedBy());
            case CIRCLES -> isInVisibleCircle(viewer, post);
            default -> false;
        };
    }

    /**
     * Determines if a user can comment on a post.
     * Currently same as canViewPost, but can be extended for future comment-specific privacy.
     *
     * @param commenter the user attempting to comment
     * @param post      the post being commented on
     * @return true if commenter can comment on the post, false otherwise
     */
    public boolean canCommentOnPost(User commenter, Post post) {
        return canViewPost(commenter, post);
    }

    /**
     * Determines if a user can like a post.
     * Currently same as canViewPost.
     *
     * @param liker the user attempting to like
     * @param post  the post being liked
     * @return true if liker can like the post, false otherwise
     */
    public boolean canLikePost(User liker, Post post) {
        return canViewPost(liker, post);
    }

    /**
     * Determines if a user can interact with another user (comment, like, etc.).
     * Enforces blocking relationships.
     *
     * @param actor  the user attempting interaction
     * @param target the user being interacted with
     * @return true if actor can interact, false if blocked
     */
    public boolean canInteract(User actor, User target) {
        if (actor == null || target == null || actor.getId().equals(target.getId())) {
            return false;
        }
        // Cannot interact if blocked bidirectionally
        return !blockRepository.isBidirectionallyBlocked(actor, target);
    }

    /**
     * Checks if follower is following the target user (approved follow only).
     *
     * @param follower the user checking follow status
     * @param target   the user being followed
     * @return true if follower follows target, false otherwise
     */
    public boolean isFollowing(User follower, User target) {
        if (follower == null || target == null || follower.getId().equals(target.getId())) {
            return false;
        }
        return followRepository.isFollowing(follower, target);
    }

    /**
     * Checks if two users are mutually following each other (both approved).
     * Used for MUTUALS_ONLY privacy level.
     *
     * @param userA first user
     * @param userB second user
     * @return true if both users follow each other, false otherwise
     */
    public boolean isMutual(User userA, User userB) {
        if (userA == null || userB == null || userA.getId().equals(userB.getId())) {
            return false;
        }
        return followRepository.areMutual(userA, userB);
    }

    /**
     * Checks if a user is in one of a post's visible circles.
     * Used for CIRCLES privacy level.
     *
     * @param user the user
     * @param post the post
     * @return true if user is a member of any circle the post is visible to
     */
    private boolean isInVisibleCircle(User user, Post post) {
        if (post.getVisibleToCircles() == null || post.getVisibleToCircles().isEmpty()) {
            return false;
        }

        return post.getVisibleToCircles().stream()
            .anyMatch(circle -> circleRepository.isMemberOfCircle(circle.getId(), user));
    }

    /**
     * Determines if a user has a public account (allows auto-following).
     *
     * @param user the user to check
     * @return true if user account is public (accountPrivate = false), false if private
     */
    public boolean isPublicAccount(User user) {
        return user != null && (user.getAccountPrivate() == null || !user.getAccountPrivate());
    }

    /**
     * Determines if a user has a private account (requires follow approval).
     *
     * @param user the user to check
     * @return true if user account is private, false otherwise
     */
    public boolean isPrivateAccount(User user) {
        return user != null && user.getAccountPrivate() != null && user.getAccountPrivate();
    }
}

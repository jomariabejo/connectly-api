package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.Follow;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.UserSettings;
import com.jomariabejo.connectly_api.repository.FollowRepository;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.UserSettingsRepository;
import com.jomariabejo.connectly_api.util.PrivacyEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PrivacyEngine privacyEngine;

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         UserSettingsRepository userSettingsRepository,
                         PrivacyEngine privacyEngine) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.privacyEngine = privacyEngine;
    }

    /**
     * Initiates a follow request from follower to following user.
     * If the target user has a public account, the follow is automatically approved.
     * If target has a private account, the follow remains pending until approved.
     *
     * @param follower  the user initiating the follow
     * @param following the user to be followed
     * @return the created Follow relationship
     * @throws IllegalArgumentException if users are the same or follow already exists
     */
    public Follow followUser(User follower, User following) {
        if (follower.getId().equals(following.getId())) {
            throw new IllegalArgumentException("A user cannot follow themselves");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);
        if (existingFollow.isPresent()) {
            throw new IllegalArgumentException("Already following this user");
        }

        Follow follow = new Follow(follower, following);

        // Check if target user has public account - auto-approve if public
        if (privacyEngine.isPublicAccount(following)) {
            follow.setApproved(true);
            follow.setRequestStatus(Follow.FollowRequestStatus.APPROVED);
            log.info("Auto-approved follow from user {} to public account user {}", follower.getId(), following.getId());
        } else {
            // Private account - require approval
            follow.setApproved(false);
            follow.setRequestStatus(Follow.FollowRequestStatus.PENDING);
            log.info("Created pending follow request from user {} to private account user {}", follower.getId(), following.getId());
        }

        return followRepository.save(follow);
    }

    /**
     * Approves a pending follow request.
     *
     * @param targetUser the user receiving the follow request
     * @param requester  the user who requested to follow
     * @return the updated Follow relationship
     * @throws IllegalArgumentException if follow doesn't exist or is not pending
     */
    public Follow approveFollowRequest(User targetUser, User requester) {
        Optional<Follow> followOpt = followRepository.findByFollowerAndFollowing(requester, targetUser);

        if (followOpt.isEmpty()) {
            throw new IllegalArgumentException("No follow request exists from this user");
        }

        Follow follow = followOpt.get();
        if (follow.getApproved()) {
            throw new IllegalArgumentException("Follow request is already approved");
        }

        follow.setApproved(true);
        follow.setRequestStatus(Follow.FollowRequestStatus.APPROVED);
        log.info("Approved follow request from user {} to user {}", requester.getId(), targetUser.getId());

        return followRepository.save(follow);
    }

    /**
     * Rejects a pending follow request.
     *
     * @param targetUser the user receiving the follow request
     * @param requester  the user who requested to follow
     * @throws IllegalArgumentException if follow doesn't exist or is not pending
     */
    public void rejectFollowRequest(User targetUser, User requester) {
        Optional<Follow> followOpt = followRepository.findByFollowerAndFollowing(requester, targetUser);

        if (followOpt.isEmpty()) {
            throw new IllegalArgumentException("No follow request exists from this user");
        }

        Follow follow = followOpt.get();
        if (follow.getApproved()) {
            throw new IllegalArgumentException("Cannot reject an already approved follow");
        }

        followRepository.delete(follow);
        log.info("Rejected follow request from user {} to user {}", requester.getId(), targetUser.getId());
    }

    /**
     * Removes a follow relationship (unfollow).
     *
     * @param follower  the user unfollowing
     * @param following the user being unfollowed
     * @throws IllegalArgumentException if follow doesn't exist
     */
    public void unfollowUser(User follower, User following) {
        Optional<Follow> followOpt = followRepository.findByFollowerAndFollowing(follower, following);

        if (followOpt.isEmpty()) {
            throw new IllegalArgumentException("Not following this user");
        }

        followRepository.delete(followOpt.get());
        log.info("User {} unfollowed user {}", follower.getId(), following.getId());
    }

    /**
     * Retrieves approved followers of a user (paginated).
     *
     * @param user     the user whose followers to retrieve
     * @param pageable pagination info
     * @return page of Follow relationships
     */
    public Page<Follow> getFollowers(User user, Pageable pageable) {
        return followRepository.findApprovedFollowersByUser(user, pageable);
    }

    /**
     * Retrieves users being followed by a user (paginated).
     *
     * @param user     the user whose following list to retrieve
     * @param pageable pagination info
     * @return page of Follow relationships
     */
    public Page<Follow> getFollowing(User user, Pageable pageable) {
        return followRepository.findApprovedFollowingByUser(user, pageable);
    }

    /**
     * Retrieves pending follow requests for a user (paginated).
     *
     * @param user     the user receiving the requests
     * @param pageable pagination info
     * @return page of pending Follow relationships
     */
    public Page<Follow> getPendingRequests(User user, Pageable pageable) {
        return followRepository.findPendingFollowRequestsByFollowing(user, pageable);
    }

    /**
     * Gets follower count for a user.
     *
     * @param user the user
     * @return count of approved followers
     */
    public long getFollowerCount(User user) {
        return followRepository.countApprovedFollowersByUser(user);
    }

    /**
     * Gets following count for a user.
     *
     * @param user the user
     * @return count of approved users being followed
     */
    public long getFollowingCount(User user) {
        return followRepository.countApprovedFollowingByUser(user);
    }

    /**
     * Checks if a user is following another user.
     *
     * @param follower the potential follower
     * @param target   the potential user being followed
     * @return true if following relationship exists and is approved
     */
    public boolean isFollowing(User follower, User target) {
        return privacyEngine.isFollowing(follower, target);
    }

    /**
     * Checks if two users are mutual followers.
     *
     * @param userA first user
     * @param userB second user
     * @return true if both follow each other
     */
    public boolean areMutual(User userA, User userB) {
        return privacyEngine.isMutual(userA, userB);
    }
}

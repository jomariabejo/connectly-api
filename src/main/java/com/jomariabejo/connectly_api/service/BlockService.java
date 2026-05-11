package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.Block;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.BlockRepository;
import com.jomariabejo.connectly_api.repository.FollowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BlockService manages blocking relationships between users.
 * When two users block each other (bidirectionally), they become invisible to each other:
 * - Neither can see the other's posts, profile, or comments
 * - Existing follows between blocked users are automatically removed
 */
@Slf4j
@Service
@Transactional
public class BlockService {

    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;

    public BlockService(BlockRepository blockRepository, FollowRepository followRepository) {
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
    }

    /**
     * Blocks a user (one-way blocking).
     * The blocker won't see the blocked user's posts/profile.
     * If there's an existing follow relationship, it's automatically removed.
     *
     * @param blocker the user initiating the block
     * @param blocked the user being blocked
     * @return the created Block relationship
     * @throws IllegalArgumentException if already blocked or self-blocking
     */
    public Block blockUser(User blocker, User blocked) {
        if (blocker.getId().equals(blocked.getId())) {
            throw new IllegalArgumentException("A user cannot block themselves");
        }

        Optional<Block> existingBlock = blockRepository.findByBlockerAndBlocked(blocker, blocked);
        if (existingBlock.isPresent()) {
            throw new IllegalArgumentException("User already blocked");
        }

        Block block = new Block(blocker, blocked);

        // Remove any follow relationships between blocker and blocked
        followRepository.findByFollowerAndFollowing(blocker, blocked)
            .ifPresent(followRepository::delete);
        followRepository.findByFollowerAndFollowing(blocked, blocker)
            .ifPresent(followRepository::delete);

        log.info("User {} blocked user {}", blocker.getId(), blocked.getId());
        return blockRepository.save(block);
    }

    /**
     * Unblocks a user.
     *
     * @param blocker the user initiating the unblock
     * @param blocked the user being unblocked
     * @throws IllegalArgumentException if block doesn't exist
     */
    public void unblockUser(User blocker, User blocked) {
        Optional<Block> blockOpt = blockRepository.findByBlockerAndBlocked(blocker, blocked);

        if (blockOpt.isEmpty()) {
            throw new IllegalArgumentException("Block relationship not found");
        }

        blockRepository.delete(blockOpt.get());
        log.info("User {} unblocked user {}", blocker.getId(), blocked.getId());
    }

    /**
     * Checks if blocker has blocked blocked user (one-way).
     *
     * @param blocker the user who might be blocking
     * @param blocked the user who might be blocked
     * @return true if blocker has blocked blocked user
     */
    public boolean isBlocked(User blocker, User blocked) {
        return blockRepository.isBlocked(blocker, blocked);
    }

    /**
     * Checks if two users are bidirectionally blocked.
     * Either user can see this relationship as blocking.
     *
     * @param userA first user
     * @param userB second user
     * @return true if userA blocks userB OR userB blocks userA
     */
    public boolean isBidirectionallyBlocked(User userA, User userB) {
        return blockRepository.isBidirectionallyBlocked(userA, userB);
    }

    /**
     * Gets all users blocked by a user.
     *
     * @param user the user
     * @return list of blocked users
     */
    public List<User> getBlockedUsers(User user) {
        return blockRepository.findAllBlockedByUser(user)
            .stream()
            .map(Block::getBlocked)
            .collect(Collectors.toList());
    }

    /**
     * Gets all users who have blocked this user.
     *
     * @param user the user
     * @return list of users blocking this user
     */
    public List<User> getBlockers(User user) {
        return blockRepository.findAllBlockers(user)
            .stream()
            .map(Block::getBlocker)
            .collect(Collectors.toList());
    }

    /**
     * Gets count of users blocked by a user.
     *
     * @param user the user
     * @return count of blocked users
     */
    public long getBlockedCount(User user) {
        return blockRepository.countBlockedByUser(user);
    }

    /**
     * Gets count of users who blocked this user.
     *
     * @param user the user
     * @return count of blockers
     */
    public long getBlockerCount(User user) {
        return blockRepository.countBlockers(user);
    }
}

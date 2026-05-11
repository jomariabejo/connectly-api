package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Block;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    // Find a block relationship between two users
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    // Find all users blocked by a user
    @Query("SELECT b FROM Block b WHERE b.blocker = :user")
    List<Block> findAllBlockedByUser(@Param("user") User user);

    // Find all users who blocked this user
    @Query("SELECT b FROM Block b WHERE b.blocked = :user")
    List<Block> findAllBlockers(@Param("user") User user);

    // Check if userA has blocked userB
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE b.blocker = :blocker AND b.blocked = :blocked")
    boolean isBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);

    // Check if userA blocks userB OR userB blocks userA (bidirectional)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blocker = :userA AND b.blocked = :userB) OR " +
           "(b.blocker = :userB AND b.blocked = :userA)")
    boolean isBidirectionallyBlocked(@Param("userA") User userA, @Param("userB") User userB);

    // Count users blocked by a user
    @Query("SELECT COUNT(b) FROM Block b WHERE b.blocker = :user")
    long countBlockedByUser(@Param("user") User user);

    // Count users who blocked this user
    @Query("SELECT COUNT(b) FROM Block b WHERE b.blocked = :user")
    long countBlockers(@Param("user") User user);
}

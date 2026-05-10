package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Follow;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // Find a follow relationship between two specific users
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // Find all approved followers of a user (users following this user with approved status)
    @Query("SELECT f FROM Follow f WHERE f.following = :user AND f.approved = true")
    List<Follow> findApprovedFollowersByUser(@Param("user") User user);

    @Query("SELECT f FROM Follow f WHERE f.following = :user AND f.approved = true")
    Page<Follow> findApprovedFollowersByUser(@Param("user") User user, Pageable pageable);

    // Find all users being followed by a specific user (approved follows only)
    @Query("SELECT f FROM Follow f WHERE f.follower = :user AND f.approved = true")
    List<Follow> findApprovedFollowingByUser(@Param("user") User user);

    @Query("SELECT f FROM Follow f WHERE f.follower = :user AND f.approved = true")
    Page<Follow> findApprovedFollowingByUser(@Param("user") User user, Pageable pageable);

    // Find pending follow requests for a user (someone trying to follow this user)
    @Query("SELECT f FROM Follow f WHERE f.following = :user AND f.approved = false")
    List<Follow> findPendingFollowRequestsByFollowing(@Param("user") User user);

    @Query("SELECT f FROM Follow f WHERE f.following = :user AND f.approved = false")
    Page<Follow> findPendingFollowRequestsByFollowing(@Param("user") User user, Pageable pageable);

    // Count approved followers of a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following = :user AND f.approved = true")
    long countApprovedFollowersByUser(@Param("user") User user);

    // Count approved users being followed by a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower = :user AND f.approved = true")
    long countApprovedFollowingByUser(@Param("user") User user);

    // Check if user1 follows user2 (approved)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Follow f " +
           "WHERE f.follower = :follower AND f.following = :following AND f.approved = true")
    boolean isFollowing(@Param("follower") User follower, @Param("following") User following);

    // Check if two users are mutual followers
    @Query("SELECT CASE WHEN COUNT(f) >= 2 THEN true ELSE false END FROM Follow f " +
           "WHERE ((f.follower = :userA AND f.following = :userB) OR " +
           "(f.follower = :userB AND f.following = :userA)) AND f.approved = true")
    boolean areMutual(@Param("userA") User userA, @Param("userB") User userB);

    // Find all approved followers' IDs for a user (for feed queries)
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following = :user AND f.approved = true")
    List<Long> findApprovedFollowerIdsByUser(@Param("user") User user);
}

package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Circle;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircleRepository extends JpaRepository<Circle, Long> {

    // Find all circles owned by a user
    List<Circle> findByOwner(User owner);

    // Find a specific circle by owner and name
    Optional<Circle> findByOwnerAndName(User owner, String name);

    // Find a specific circle by owner and ID
    @Query("SELECT c FROM Circle c WHERE c.owner = :owner AND c.id = :circleId")
    Optional<Circle> findByOwnerAndId(@Param("owner") User owner, @Param("circleId") Long circleId);

    // Check if a user is a member of a specific circle
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Circle c " +
           "JOIN c.members m WHERE c.id = :circleId AND m = :user")
    boolean isMemberOfCircle(@Param("circleId") Long circleId, @Param("user") User user);

    // Find all circles that include a specific user as a member (where user is member)
    @Query("SELECT c FROM Circle c JOIN c.members m WHERE m = :user")
    List<Circle> findCirclesContainingMember(@Param("user") User user);

    // Count members in a circle
    @Query("SELECT COUNT(m) FROM Circle c JOIN c.members m WHERE c.id = :circleId")
    long countMembers(@Param("circleId") Long circleId);
}

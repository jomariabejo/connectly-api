package com.jomariabejo.connectly_api.repository;


import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Active user queries (soft-delete filtering)
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsername(@Param("username") String username);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsername(@Param("username") String username);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsAnyByUsername(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean existsAnyByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE (u.username = :username OR u.email = :email) AND u.deletedAt IS NULL")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.verificationToken = :token AND u.deletedAt IS NULL")
    Optional<User> findByVerificationToken(@Param("token") String token);

    // Pagination methods with soft-delete filtering
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAll(Pageable pageable);

    // Filtering methods with soft-delete filtering
    @Query("SELECT u FROM User u WHERE " +
            "u.deletedAt IS NULL AND " +
            "(:username IS NULL OR u.username LIKE %:username%) AND " +
            "(:email IS NULL OR u.email LIKE %:email%) AND " +
            "(:firstName IS NULL OR u.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR u.lastName LIKE %:lastName%)")
    Page<User> findWithFilters(
            @Param("username") String username,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            Pageable pageable);

    // Soft-delete specific queries
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
    List<User> findDeletedUsers();

    @Query("SELECT u FROM User u WHERE u.scheduledDeletionAt IS NOT NULL AND u.scheduledDeletionAt <= CURRENT_TIMESTAMP")
    List<User> findUsersScheduledForDeletion();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt > :deletedAfter")
    List<User> findRecentlyDeletedUsers(@Param("deletedAfter") LocalDateTime deletedAfter);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveUserById(@Param("id") Long id);
}

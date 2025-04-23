package com.jomariabejo.connectly_api.repository;


import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    Optional<User> findByUsername(String username);
//    Optional<User> findByEmail(String email);
//    boolean existsByUsername(String username);
//    boolean existsByEmail(String email);
//
//    // Method to find a user by either username or email
//    Optional<User> findByUsernameOrEmail(String username, String email);
//
//    Optional<User> findByVerificationToken(String token);
//
//    void createVerificationToken(User user, String token);
//
//    VerificationToken getVerificationToken(String VerificationToken);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByVerificationToken(String token);
}
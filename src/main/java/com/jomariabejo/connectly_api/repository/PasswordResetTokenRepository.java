package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByOtp(String otp);

    @Query("SELECT p FROM PasswordResetToken p WHERE p.user = :user AND p.isUsed = false AND p.expiryDate > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findUnexpiredByUser(@Param("user") User user);

    @Query("SELECT p FROM PasswordResetToken p WHERE p.user = :user AND p.isUsed = false")
    List<PasswordResetToken> findAllUnusedByUser(@Param("user") User user);

    @Query("SELECT p FROM PasswordResetToken p WHERE p.user = :user")
    List<PasswordResetToken> findAllByUser(@Param("user") User user);

    @Query("SELECT p FROM PasswordResetToken p WHERE p.expiryDate < CURRENT_TIMESTAMP AND p.isUsed = false")
    List<PasswordResetToken> findExpiredTokens();

    // Used when permanently deleting an account -- see UserService.permanentlyDeleteUser.
    void deleteAllByUser(User user);
}

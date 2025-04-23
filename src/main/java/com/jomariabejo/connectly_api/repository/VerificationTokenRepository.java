package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    VerificationToken findByToken(String token);

    VerificationToken findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.expiryDate < ?1")
    List<VerificationToken> findAllExpiredTokens(Date now);
}
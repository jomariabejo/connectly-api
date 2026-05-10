package com.jomariabejo.connectly_api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Calendar;

@Data
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
    private static final int EXPIRATION_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = true)
    private String otp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType tokenType;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

    @Column(nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    private boolean isUsed = false;

    @Column(nullable = false)
    private int attemptCount = 0;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.tokenType = TokenType.LINK;
        this.isUsed = false;
        this.attemptCount = 0;
        this.expiryDate = calculateExpiryDate(EXPIRATION_MINUTES);
    }

    public PasswordResetToken(String token, String otp, User user, TokenType tokenType) {
        this.token = token;
        this.otp = otp;
        this.user = user;
        this.tokenType = tokenType;
        this.isUsed = false;
        this.attemptCount = 0;
        this.expiryDate = calculateExpiryDate(EXPIRATION_MINUTES);
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(java.util.Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public boolean isExpired() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime().after(this.expiryDate);
    }

    public enum TokenType {
        LINK,
        OTP
    }
}

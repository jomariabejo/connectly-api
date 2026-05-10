package com.jomariabejo.connectly_api.util;

import java.security.SecureRandom;

public class OtpGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int MAX_OTP_VALUE = 1000000; // 999999 + 1

    public static String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(MAX_OTP_VALUE);
        return String.format("%06d", otp);
    }

    public static String generateSecureToken() {
        return java.util.UUID.randomUUID().toString();
    }
}

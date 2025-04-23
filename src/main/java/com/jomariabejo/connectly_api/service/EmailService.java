package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.GlobalExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String link) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setFrom("noreply@yourdomain.com"); // Add this line
            mailMessage.setSubject("Complete Registration");
            mailMessage.setText("To confirm your account, please click here: " + link);

            logger.info("Sending verification email to: " + toEmail);
            mailSender.send(mailMessage);
            logger.info("Verification email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send verification email", e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}
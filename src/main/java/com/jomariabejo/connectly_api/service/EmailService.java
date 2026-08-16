package com.jomariabejo.connectly_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

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

    public void sendPasswordResetEmailWithLink(String toEmail, String resetLink) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setFrom("noreply@yourdomain.com");
            mailMessage.setSubject("Password Reset Request");
            
            String emailBody = "You requested a password reset for your Connectly account.\n\n"
                    + "Click the link below to reset your password:\n"
                    + resetLink + "\n\n"
                    + "This link will expire in 15 minutes.\n\n"
                    + "If you did not request this, please ignore this email and your password will remain unchanged.\n"
                    + "For security reasons, we'll never ask you for your password via email.\n\n"
                    + "For your protection, this link is unique to your account and can only be used once.";
            
            mailMessage.setText(emailBody);

            logger.info("Sending password reset email to: " + toEmail);
            mailSender.send(mailMessage);
            logger.info("Password reset email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send password reset email", e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setFrom("noreply@yourdomain.com");
            mailMessage.setSubject("Your Password Reset Code");
            
            String emailBody = "You requested a password reset for your Connectly account.\n\n"
                    + "Your password reset verification code is:\n\n"
                    + otp + "\n\n"
                    + "This code will expire in 15 minutes.\n"
                    + "You can attempt to enter this code 3 times before requesting a new one.\n\n"
                    + "If you did not request this, please ignore this email and your password will remain unchanged.\n"
                    + "For security reasons, we'll never ask you for your password via email.\n\n"
                    + "Do not share this code with anyone. Connectly support will never ask for this code.";
            
            mailMessage.setText(emailBody);

            logger.info("Sending password reset OTP to: " + toEmail);
            mailSender.send(mailMessage);
            logger.info("Password reset OTP email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP email", e);
            throw new RuntimeException("Failed to send password reset OTP email", e);
        }
    }
}
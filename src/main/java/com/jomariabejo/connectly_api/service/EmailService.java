package com.jomariabejo.connectly_api.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Map;

@Service
public class EmailService {
    private static final String FROM_ADDRESS = "noreply@yourdomain.com";
    private static final String LOGO_CONTENT_ID = "connectlyLogo";
    private static final String LOGO_RESOURCE_PATH = "assets/MMDC_LOGO.png";
    private static final String VERIFICATION_TEMPLATE = "assets/emails/connectly-registration.hbs";
    private static final String PASSWORD_RESET_LINK_TEMPLATE = "assets/emails/password-reset-link.hbs";
    private static final String PASSWORD_RESET_OTP_TEMPLATE = "assets/emails/password-reset-otp.hbs";

    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.fail-on-error:false}")
    private boolean failOnError;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String link) {
        logger.info("Sending verification email to: {}", toEmail);
        sendTemplatedEmail(
                toEmail,
                "Complete your Connectly registration",
                VERIFICATION_TEMPLATE,
                "To confirm your Connectly account, open this link: " + link,
                Map.of(
                        "preheader", "Confirm your account and start using Connectly.",
                        "verificationLink", link
                ),
                "verification email",
                "Verification link: " + link
        );
    }

    public void sendPasswordResetEmailWithLink(String toEmail, String resetLink) {
        String plainText = "You requested a password reset for your Connectly account.\n\n"
                + "Click the link below to reset your password:\n"
                + resetLink + "\n\n"
                + "This link will expire in 15 minutes.\n\n"
                + "If you did not request this, please ignore this email and your password will remain unchanged.\n"
                + "For security reasons, we'll never ask you for your password via email.\n\n"
                + "For your protection, this link is unique to your account and can only be used once.";

        logger.info("Sending password reset email to: {}", toEmail);
        sendTemplatedEmail(
                toEmail,
                "Reset your Connectly password",
                PASSWORD_RESET_LINK_TEMPLATE,
                plainText,
                Map.of(
                        "preheader", "Use your secure password reset link before it expires.",
                        "resetLink", resetLink
                ),
                "password reset email",
                "Password reset link: " + resetLink
        );
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        String plainText = "You requested a password reset for your Connectly account.\n\n"
                + "Your password reset verification code is:\n\n"
                + otp + "\n\n"
                + "This code will expire in 15 minutes.\n"
                + "You can attempt to enter this code 3 times before requesting a new one.\n\n"
                + "If you did not request this, please ignore this email and your password will remain unchanged.\n"
                + "For security reasons, we'll never ask you for your password via email.\n\n"
                + "Do not share this code with anyone. Connectly support will never ask for this code.";

        logger.info("Sending password reset OTP to: {}", toEmail);
        sendTemplatedEmail(
                toEmail,
                "Your Connectly password reset code",
                PASSWORD_RESET_OTP_TEMPLATE,
                plainText,
                Map.of(
                        "preheader", "Use this one-time code to reset your password.",
                        "otp", otp
                ),
                "password reset OTP email",
                "Password reset OTP: " + otp
        );
    }

    private void sendTemplatedEmail(
            String toEmail,
            String subject,
            String templatePath,
            String plainText,
            Map<String, String> templateValues,
            String emailType,
            String fallbackContent
    ) {
        String html = renderTemplate(templatePath, templateValues);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(toEmail);
            helper.setFrom(FROM_ADDRESS);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            addLogoIfPresent(helper);

            mailSender.send(message);
            logger.info("{} sent successfully", emailType);
        } catch (Exception e) {
            if (failOnError) {
                logger.error("Failed to send {}", emailType, e);
                throw new RuntimeException("Failed to send " + emailType, e);
            }

            logger.warn(
                    "Failed to send {}. Continuing because app.mail.fail-on-error=false. {}",
                    emailType,
                    fallbackContent,
                    e
            );
        }
    }

    private String renderTemplate(String templatePath, Map<String, String> values) {
        try {
            ClassPathResource template = new ClassPathResource(templatePath);
            String rendered = StreamUtils.copyToString(template.getInputStream(), StandardCharsets.UTF_8);

            rendered = rendered.replace("{{logoCid}}", "cid:" + LOGO_CONTENT_ID);
            rendered = rendered.replace("{{year}}", String.valueOf(Year.now().getValue()));
            for (Map.Entry<String, String> entry : values.entrySet()) {
                rendered = rendered.replace(
                        "{{" + entry.getKey() + "}}",
                        escapeHtml(entry.getValue())
                );
            }

            return rendered;
        } catch (IOException e) {
            throw new IllegalStateException("Email template not found: " + templatePath, e);
        }
    }

    private void addLogoIfPresent(MimeMessageHelper helper) throws jakarta.mail.MessagingException {
        ClassPathResource logo = new ClassPathResource(LOGO_RESOURCE_PATH);
        if (logo.exists()) {
            helper.addInline(LOGO_CONTENT_ID, logo);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

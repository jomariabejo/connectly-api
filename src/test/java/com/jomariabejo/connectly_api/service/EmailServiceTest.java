package com.jomariabejo.connectly_api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EmailService}.
 *
 * <p>The {@link JavaMailSender} is mocked, so no SMTP connection is ever attempted. Each happy
 * path captures the {@link SimpleMailMessage} handed to the sender and asserts the full envelope
 * -- recipient, hard-coded from address, subject and that the body carries the link or code the
 * caller supplied. Each failure path stubs {@code send} to throw a {@link MailSendException} and
 * asserts the service wraps it into a {@link RuntimeException} with the documented message,
 * keeping the original failure as the cause. {@code isExactlyInstanceOf} matters there:
 * {@link MailSendException} itself is a {@link RuntimeException}, so a plain instanceof check
 * could not tell a wrapped failure from an unwrapped one.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    private static final String RECIPIENT = "someone@example.com";
    private static final String FROM = "noreply@yourdomain.com";

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("addresses the message correctly and embeds the confirmation link")
        void sendsLink() {
            String link = "http://localhost:8080/auth/verify?token=abc-123";

            emailService.sendVerificationEmail(RECIPIENT, link);

            SimpleMailMessage message = sentMessage();
            assertThat(message.getTo()).containsExactly(RECIPIENT);
            assertThat(message.getFrom()).isEqualTo(FROM);
            assertThat(message.getSubject()).isEqualTo("Complete Registration");
            assertThat(message.getText()).contains(link);
        }

        @Test
        @DisplayName("wraps a mail failure into a RuntimeException with the documented message")
        void wrapsSendFailure() {
            doThrow(new MailSendException("boom")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendVerificationEmail(RECIPIENT, "http://link"))
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to send verification email")
                    .hasCauseInstanceOf(MailSendException.class);
        }
    }

    @Nested
    @DisplayName("sendPasswordResetEmailWithLink")
    class SendPasswordResetEmailWithLink {

        @Test
        @DisplayName("addresses the message correctly and embeds the reset link")
        void sendsResetLink() {
            String resetLink = "http://localhost:8080/reset-password?token=reset-token";

            emailService.sendPasswordResetEmailWithLink(RECIPIENT, resetLink);

            SimpleMailMessage message = sentMessage();
            assertThat(message.getTo()).containsExactly(RECIPIENT);
            assertThat(message.getFrom()).isEqualTo(FROM);
            assertThat(message.getSubject()).isEqualTo("Password Reset Request");
            assertThat(message.getText()).contains(resetLink);
        }

        @Test
        @DisplayName("wraps a mail failure into a RuntimeException with the documented message")
        void wrapsSendFailure() {
            doThrow(new MailSendException("boom")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendPasswordResetEmailWithLink(RECIPIENT, "http://link"))
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to send password reset email")
                    .hasCauseInstanceOf(MailSendException.class);
        }
    }

    @Nested
    @DisplayName("sendPasswordResetOtp")
    class SendPasswordResetOtp {

        @Test
        @DisplayName("addresses the message correctly and embeds the one-time code")
        void sendsOtp() {
            String otp = "123456";

            emailService.sendPasswordResetOtp(RECIPIENT, otp);

            SimpleMailMessage message = sentMessage();
            assertThat(message.getTo()).containsExactly(RECIPIENT);
            assertThat(message.getFrom()).isEqualTo(FROM);
            assertThat(message.getSubject()).isEqualTo("Your Password Reset Code");
            assertThat(message.getText()).contains(otp);
        }

        @Test
        @DisplayName("wraps a mail failure into a RuntimeException with the documented message")
        void wrapsSendFailure() {
            doThrow(new MailSendException("boom")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendPasswordResetOtp(RECIPIENT, "123456"))
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to send password reset OTP email")
                    .hasCauseInstanceOf(MailSendException.class);
        }
    }
}

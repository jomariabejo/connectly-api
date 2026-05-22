package com.jomariabejo.connectly_api.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void sendVerificationEmailContinuesWhenMailServerIsUnavailableAndFailOnErrorIsFalse() {
        JavaMailSender mailSender = failingMailSender();
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "failOnError", false);

        assertThatCode(() -> emailService.sendVerificationEmail(
                "test@example.com",
                "http://localhost:3000/verify-email?token=test-token",
                "123456",
                "http://localhost:3000/check-email"
        )).doesNotThrowAnyException();
    }

    @Test
    void sendVerificationEmailThrowsWhenFailOnErrorIsTrue() {
        JavaMailSender mailSender = failingMailSender();
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "failOnError", true);

        assertThatThrownBy(() -> emailService.sendVerificationEmail(
                "test@example.com",
                "http://localhost:3000/verify-email?token=test-token",
                "123456",
                "http://localhost:3000/check-email"
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send verification email");
    }

    private JavaMailSender failingMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("Connection refused"))
                .when(mailSender)
                .send(any(MimeMessage.class));
        return mailSender;
    }
}

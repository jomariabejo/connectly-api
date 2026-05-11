package com.jomariabejo.connectly_api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class EmailServiceTest {

    @Test
    void sendVerificationEmailContinuesWhenMailServerIsUnavailableAndFailOnErrorIsFalse() {
        JavaMailSender mailSender = failingMailSender();
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "failOnError", false);

        assertThatCode(() -> emailService.sendVerificationEmail(
                "test@example.com",
                "http://localhost:8080/v1/auth/verify?token=test-token"
        )).doesNotThrowAnyException();
    }

    @Test
    void sendVerificationEmailThrowsWhenFailOnErrorIsTrue() {
        JavaMailSender mailSender = failingMailSender();
        EmailService emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "failOnError", true);

        assertThatThrownBy(() -> emailService.sendVerificationEmail(
                "test@example.com",
                "http://localhost:8080/v1/auth/verify?token=test-token"
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send verification email");
    }

    private JavaMailSender failingMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Connection refused"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));
        return mailSender;
    }
}

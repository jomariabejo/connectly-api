package com.jomariabejo.connectly_api.registration.listener;

import com.jomariabejo.connectly_api.common.ApiUrlBuilder;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    private final UserService userService;
    private final VerificationTokenService tokenService;
    private final MessageSource messages;
    private final JavaMailSender mailSender;
    private final ApiUrlBuilder apiUrlBuilder;

    private final Logger logger = LoggerFactory.getLogger(RegistrationListener.class);

    @Autowired
    public RegistrationListener(UserService userService,
                                VerificationTokenService tokenService,
                                MessageSource messages,
                                JavaMailSender mailSender,
                                ApiUrlBuilder apiUrlBuilder) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.messages = messages;
        this.mailSender = mailSender;
        this.apiUrlBuilder = apiUrlBuilder;
    }

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }

    private void confirmRegistration(OnRegistrationCompleteEvent event) {
        User user = event.getUser();
        String token = UUID.randomUUID().toString();

        // Create verification token
        tokenService.createVerificationToken(user, token);

        String recipientAddress = user.getEmail();
        String subject = "Registration Confirmation";
        String confirmationUrl = apiUrlBuilder.authVerifyUrl(token);

        try {
            String message = messages.getMessage("message.regSucc", null, event.getLocale());

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(recipientAddress);
            email.setSubject(subject);
            email.setText(message + "\r\n" + confirmationUrl);

            logger.info("Sending registration email to: " + recipientAddress);
            mailSender.send(email);
            logger.info("Registration email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send registration email", e);
        }
    }
}

package com.jomariabejo.connectly_api.registration.listener;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists the {@code verification_token} row backing {@code GET /auth/registrationConfirm}.
 *
 * <p>This listener used to generate its own {@link java.util.UUID} here, so a single registration
 * produced <b>two unrelated tokens</b>: the one on {@code app_user.verification_token} that
 * {@code /auth/verify} checks and emails, and this one, which only
 * {@code /auth/registrationConfirm} checks and which nobody was ever sent. The confirm endpoint
 * therefore rejected the token users actually received.
 *
 * <p>It now reuses the token already assigned during signup, so both endpoints accept the same
 * emailed value.
 *
 * <p>It also used to send a <i>second</i> registration email carrying its dead token.
 * {@code AuthenticationService.signup} already sends the working one through {@code EmailService},
 * so that duplicate has been removed.
 */
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationListener.class);

    private final VerificationTokenService tokenService;

    public RegistrationListener(VerificationTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }

    private void confirmRegistration(OnRegistrationCompleteEvent event) {
        User user = event.getUser();
        String token = user.getVerificationToken();

        if (token == null) {
            logger.warn("No verification token on {} -- skipping token row; /auth/registrationConfirm "
                    + "will not work for this account", user.getEmail());
            return;
        }

        tokenService.createVerificationToken(user, token);
        logger.info("Stored verification token row for {}", user.getEmail());
    }
}

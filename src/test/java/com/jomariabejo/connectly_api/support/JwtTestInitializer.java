package com.jomariabejo.connectly_api.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String encodedKey = Base64.getEncoder().encodeToString(key);
        TestPropertyValues.of("jwt.secret-key=" + encodedKey)
                .applyTo(applicationContext);
    }
}

package com.jomariabejo.connectly_api.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiUrlBuilderTest {

    @Test
    void authVerifyUrlIncludesApiContextPathAndV1Prefix() {
        ApiUrlBuilder builder = new ApiUrlBuilder("http://localhost:8080/api");

        assertThat(builder.authVerifyUrl("abc-123"))
                .isEqualTo("http://localhost:8080/api/v1/auth/verify?token=abc-123");
    }

    @Test
    void authResetPasswordUrlIncludesApiContextPathAndV1Prefix() {
        ApiUrlBuilder builder = new ApiUrlBuilder("http://localhost:8082/api/");

        assertThat(builder.authResetPasswordUrl("reset-token"))
                .isEqualTo("http://localhost:8082/api/v1/auth/reset-password?token=reset-token");
    }
}

package com.jomariabejo.connectly_api.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendUrlBuilderTest {

    @Test
    void signupUrlPointsToFrontendSignupPage() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("http://localhost:3000");

        assertThat(builder.signupUrl()).isEqualTo("http://localhost:3000/signup");
    }
}

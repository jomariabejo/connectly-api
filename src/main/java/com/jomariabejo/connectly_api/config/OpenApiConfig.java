package com.jomariabejo.connectly_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI metadata.
 *
 * <p>springdoc scans the {@code @RestController}s at runtime and serves the result at
 * {@code /v3/api-docs} (JSON), {@code /v3/api-docs.yaml} (YAML) and {@code /swagger-ui.html}
 * (the interactive console). This class supplies the document-level metadata plus the
 * {@code bearerAuth} scheme that puts the <b>Authorize</b> button in Swagger UI, so a token
 * from {@code POST /auth/login} can be pasted once and reused across every secured endpoint.
 *
 * <p>The Swagger routes are allow-listed in {@link SecurityConfiguration}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Connectly API",
                version = "0.0.1-SNAPSHOT",
                description = """
                        Backend for a social platform: users, posts, comments and likes, with \
                        JWT authentication, email verification, password reset (link or OTP) \
                        and soft account deletion with a 30-day grace period.

                        Send `Authorization: Bearer <token>` on every endpoint outside `/auth/**`. \
                        Obtain a token from `POST /auth/login`.""",
                license = @License(name = "MIT", url = "https://github.com/jomariabejo/connectly-api/blob/main/LICENSE"),
                contact = @Contact(name = "jomariabejo", url = "https://github.com/jomariabejo/connectly-api")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Local development")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT obtained from POST /auth/login. Paste the raw token; the Bearer prefix is added for you.",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}

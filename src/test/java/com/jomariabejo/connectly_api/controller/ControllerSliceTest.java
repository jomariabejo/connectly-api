package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.config.JwtAuthenticationFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shared setup for the controller slice tests.
 *
 * <p>Two things every slice in this package needs:
 *
 * <ul>
 *   <li>{@code addFilters = false} -- these tests assert the HTTP contract (routing, validation,
 *       status codes, JSON shape), not the security wiring, so the JWT filter chain is bypassed.
 *   <li>Excluding {@link JwtAuthenticationFilter} from the scan. {@code @WebMvcTest} pulls in
 *       {@code Filter} components even when filters are disabled, and that one needs a
 *       {@code JwtService} and a {@code UserDetailsService} that a slice does not provide -- so
 *       without this the context fails to start before a single request is made.
 * </ul>
 *
 * <p>Collaborators are supplied per test class with {@code @MockitoBean}; Spring Boot 3.4
 * deprecated {@code @MockBean} in favour of it.
 *
 * <p>Usage: {@code @ControllerSliceTest(PostController.class)}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@WebMvcTest(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
public @interface ControllerSliceTest {

    /** The controller under test. */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}

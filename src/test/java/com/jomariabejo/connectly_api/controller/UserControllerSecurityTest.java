package com.jomariabejo.connectly_api.controller;

import com.jayway.jsonpath.JsonPath;
import com.jomariabejo.connectly_api.config.JwtAccessDeniedHandler;
import com.jomariabejo.connectly_api.config.JwtAuthenticationEntryPoint;
import com.jomariabejo.connectly_api.config.SecurityConfiguration;
import com.jomariabejo.connectly_api.exception.GlobalExceptionHandler;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-slice tests proving that {@code @EnableMethodSecurity} guards the {@code /users/admin/**}
 * routes.
 *
 * <p><b>Why this slice runs with filters ON while every other slice runs with them OFF.</b> The
 * shared {@link ControllerSliceTest} annotation disables the filter chain because those tests assert
 * the HTTP contract (routing, validation, status codes, JSON shape) and deliberately ignore
 * security. This class exists for the opposite reason: it is the regression guard for the
 * missing-{@code @EnableMethodSecurity} privilege-escalation bug. The admin routes live at
 * {@code /users/admin/**}, which matches none of the {@code requestMatchers} rules in
 * {@link SecurityConfiguration} — they fall through to {@code anyRequest().authenticated()}, so the
 * {@code @PreAuthorize("hasRole('ADMIN')")} method guards are their <i>only</i> role check. Without
 * {@code @EnableMethodSecurity} those annotations are silently ignored and any authenticated USER
 * can delete accounts. Proving the guard therefore requires the real security wiring: a raw
 * {@code @WebMvcTest} with filters enabled and the production {@link SecurityConfiguration}
 * imported, so denials travel the same path they travel in production.
 *
 * <p><b>Bean roster.</b> {@link SecurityConfiguration} is imported together with the real
 * {@link JwtAuthenticationEntryPoint} (401 with a JSON {@code ErrorResponse} when the caller is
 * unknown) and {@link JwtAccessDeniedHandler} (403 with the same shape when the caller is known but
 * lacks the role), because plain {@code @Component}s are not scanned by {@code @WebMvcTest}. The
 * scanned {@code JwtAuthenticationFilter} (which {@code @WebMvcTest} pulls in as a {@code Filter}
 * component) needs {@link JwtService} and {@link UserDetailsService}, and the imported
 * {@link SecurityConfiguration} needs an {@link AuthenticationProvider}; all three are Mockito
 * stubs — no request here carries a bearer token, the caller identity comes from
 * {@code @WithMockUser}.
 *
 * <p><b>Why {@link GlobalExceptionHandler} is excluded from the scan.</b> Its catch-all
 * {@code @ExceptionHandler(Exception.class)} runs inside the DispatcherServlet, <i>before</i> the
 * exception can reach the filter chain's {@code ExceptionTranslationFilter}, and it claims the
 * {@code AuthorizationDeniedException} thrown by the {@code @PreAuthorize} interceptor — turning the
 * denial into a 500 and hiding the {@link JwtAccessDeniedHandler} contract this class pins down.
 * With the advice sliced out, the denial propagates to the security layer and the 401/403 split
 * described in {@link SecurityConfiguration} is observable. The escalation guard itself does not
 * depend on the advice either way: the tests also verify the admin service methods are never
 * invoked for a denied caller.
 *
 * <p>CSRF is disabled in {@link SecurityConfiguration}, so the DELETE request needs no
 * {@code csrf()} post-processor.
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class))
@Import({SecurityConfiguration.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("UserController admin-route security (filters enabled)")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Controller collaborators.
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    // Needed by the scanned JwtAuthenticationFilter.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // Needed by the imported SecurityConfiguration.
    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser // plain ROLE_USER
    @DisplayName("GET /users/admin/users/scheduled-deletion as plain USER is denied with the JwtAccessDeniedHandler 403 body")
    void plainUserCannotListScheduledDeletions() throws Exception {
        long before = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(get("/users/admin/users/scheduled-deletion"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource."))
                .andReturn();

        long after = System.currentTimeMillis();
        Number timestamp = JsonPath.read(result.getResponse().getContentAsString(), "$.timestamp");
        assertThat(timestamp.longValue()).isBetween(before, after);

        // The denial happened before the handler ran -- the admin query was never executed.
        verify(userService, never()).getUsersScheduledForDeletion();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /users/admin/users/scheduled-deletion as ADMIN passes the guard and answers 200")
    void adminCanListScheduledDeletions() throws Exception {
        when(userService.getUsersScheduledForDeletion()).thenReturn(List.of());

        mockMvc.perform(get("/users/admin/users/scheduled-deletion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userService).getUsersScheduledForDeletion();
    }

    @Test
    @WithMockUser // plain ROLE_USER
    @DisplayName("DELETE /users/admin/users/{id} as plain USER is denied with 403 and never touches the service")
    void plainUserCannotDeleteAccounts() throws Exception {
        mockMvc.perform(delete("/users/admin/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // The privilege-escalation guard: no lookup, no soft delete, no permanent delete.
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /users/admin/users/scheduled-deletion unauthenticated is challenged with the JwtAuthenticationEntryPoint 401 body")
    void anonymousCallerIsChallengedWith401() throws Exception {
        long before = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(get("/users/admin/users/scheduled-deletion"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication required. Send a bearer token from POST /auth/login."))
                .andReturn();

        long after = System.currentTimeMillis();
        Number timestamp = JsonPath.read(result.getResponse().getContentAsString(), "$.timestamp");
        assertThat(timestamp.longValue()).isBetween(before, after);

        verifyNoInteractions(userService);
    }
}

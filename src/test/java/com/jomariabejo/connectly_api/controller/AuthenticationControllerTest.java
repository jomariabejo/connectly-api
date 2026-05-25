package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.common.FrontendUrlBuilder;
import com.jomariabejo.connectly_api.config.JwtAuthenticationFilter;
import com.jomariabejo.connectly_api.config.SecurityConfiguration;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.tenant_api.config.TenantFilter;
import com.jomariabejo.connectly_api.tenant_api.service.ActorRegistrationService;
import com.jomariabejo.connectly_api.tenant_api.service.LoginRedirectService;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.TenantInvitationService;
import com.jomariabejo.connectly_api.tenant_api.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TenantFilter.class, FrontendUrlBuilder.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private ActorRegistrationService actorRegistrationService;

    @MockitoBean
    private TenantInvitationService tenantInvitationService;

    @MockitoBean
    private LoginRedirectService loginRedirectService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TenantContextService tenantContextService;

    @Test
    void getRegistrationRedirectsToFrontendSignup() throws Exception {
        mockMvc.perform(get("/v1/auth/registration"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000/signup"));
    }

    @Test
    void patchRegistrationReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/v1/auth/registration"))
                .andExpect(status().isMethodNotAllowed());
    }
}

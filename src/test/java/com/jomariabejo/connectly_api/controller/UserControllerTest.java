package com.jomariabejo.connectly_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.config.JwtAuthenticationFilter;
import com.jomariabejo.connectly_api.config.SecurityConfiguration;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.service.UserSettingsService;
import com.jomariabejo.connectly_api.tenant_api.config.TenantFilter;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TenantFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TenantContextService tenantContextService;

    @Test
    @WithMockUser
    void reactivationRequiresToken() throws Exception {
        mockMvc.perform(post("/v1/users/reactivate")
                        .contentType("application/json")
                        .content("{\"reactivationToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void successfulReactivationReturnsNoContent() throws Exception {
        mockMvc.perform(post("/v1/users/reactivate")
                        .contentType("application/json")
                        .content("{\"reactivationToken\":\"token-123\"}"))
                .andExpect(status().isNoContent());

        verify(userService).reactivateAccount("token-123");
    }
}

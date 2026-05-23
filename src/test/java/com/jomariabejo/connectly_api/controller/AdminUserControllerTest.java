package com.jomariabejo.connectly_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.config.JwtAuthenticationFilter;
import com.jomariabejo.connectly_api.config.SecurityConfiguration;
import com.jomariabejo.connectly_api.dto.AdminDeleteAccountRequestDto;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TenantFilter.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

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
    @WithMockUser(roles = "USER")
    void regularUserCannotDeleteUsers() throws Exception {
        mockMvc.perform(delete("/v1/admin/users/42"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDeleteReturnsNoContent() throws Exception {
        AdminDeleteAccountRequestDto request = new AdminDeleteAccountRequestDto(true, "cleanup", null);

        mockMvc.perform(delete("/v1/admin/users/42")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).adminDeleteUser(eq(42L), any(AdminDeleteAccountRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void extensionRequiresPositiveDays() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/42/deletion-grace-period")
                        .contentType("application/json")
                        .content("{\"extensionDays\":0}"))
                .andExpect(status().isBadRequest());
    }
}

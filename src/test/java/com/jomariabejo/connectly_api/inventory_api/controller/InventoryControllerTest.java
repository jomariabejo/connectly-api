package com.jomariabejo.connectly_api.inventory_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.config.JwtAuthenticationFilter;
import com.jomariabejo.connectly_api.config.SecurityConfiguration;
import com.jomariabejo.connectly_api.inventory_api.dto.CreateInventoryItemRequest;
import com.jomariabejo.connectly_api.inventory_api.dto.InventoryItemDto;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.tenant_api.config.TenantFilter;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TenantFilter.class})
class InventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean(name = "handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    @MockitoBean
    private TenantContextService tenantContextService;

    @BeforeEach
    void setUpTenantContext() {
        TenantContext.set(1L, TenantRole.ADMIN, EnumSet.allOf(ProductCode.class));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUserCanReadActiveInventory() throws Exception {
        when(inventoryService.getActiveInventory()).thenReturn(List.of(InventoryItemDto.builder()
                .sku("NOTEBOOK-1")
                .name("Notebook")
                .unitPrice(new BigDecimal("49.98"))
                .currency("PHP")
                .onHandQuantity(10)
                .reservedQuantity(2)
                .availableQuantity(8)
                .active(true)
                .build()));

        mockMvc.perform(get("/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("NOTEBOOK-1"))
                .andExpect(jsonPath("$[0].availableQuantity").value(8));
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotCreateInventoryItem() throws Exception {
        mockMvc.perform(post("/v1/admin/inventory")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateInventoryItem() throws Exception {
        when(inventoryService.createInventoryItem(any(CreateInventoryItemRequest.class))).thenReturn(InventoryItemDto.builder()
                .sku("NOTEBOOK-1")
                .name("Notebook")
                .unitPrice(new BigDecimal("49.98"))
                .currency("PHP")
                .onHandQuantity(10)
                .reservedQuantity(0)
                .availableQuantity(10)
                .active(true)
                .build());

        mockMvc.perform(post("/v1/admin/inventory")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("NOTEBOOK-1"));
    }

    private CreateInventoryItemRequest createRequest() {
        return CreateInventoryItemRequest.builder()
                .sku("NOTEBOOK-1")
                .name("Notebook")
                .unitPrice(new BigDecimal("49.98"))
                .currency("PHP")
                .onHandQuantity(10)
                .build();
    }
}

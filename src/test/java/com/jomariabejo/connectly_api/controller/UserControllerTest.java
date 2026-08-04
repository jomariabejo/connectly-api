package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for {@link UserController}, covering the profile endpoints, the
 * soft-delete/reactivate lifecycle, and the admin routes.
 *
 * <p>See {@link ControllerSliceTest} for what the slice does and does not cover. Because that
 * annotation disables the filter chain, the {@code @PreAuthorize} checks on the admin routes are
 * not enforced here -- {@code @WithMockUser} documents the intended caller rather than proving the
 * rule. The role rules themselves live in {@code SecurityConfiguration} and are exercised against a
 * running server.
 */
@ControllerSliceTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    private static Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    @Test
    @DisplayName("GET /users/me returns the caller's profile")
    void returnsOwnProfile() throws Exception {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("someone@example.com"));
    }

    @Test
    @DisplayName("GET /users/ needs the trailing slash and returns every user")
    void listsAllUsers() throws Exception {
        when(userService.allUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/users/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /users/paginated applies the id,asc page-10 default")
    void appliesPageableDefaults() throws Exception {
        when(userService.getAllUsersPaginated(any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(user), 0, 10, 1, 1));

        mockMvc.perform(get("/users/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsersPaginated(pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("GET /users/paginated switches to the filtered query when a filter is supplied")
    void switchesToFilteredQuery() throws Exception {
        when(userService.getAllUsersWithFilters(any(UserFilterDto.class), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(user), 0, 10, 1, 1));

        mockMvc.perform(get("/users/paginated").param("email", "someone@"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<UserFilterDto> filter = ArgumentCaptor.forClass(UserFilterDto.class);
        verify(userService).getAllUsersWithFilters(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().getEmail()).isEqualTo("someone@");
        verify(userService, never()).getAllUsersPaginated(any());
    }

    @Test
    @DisplayName("DELETE /users/me answers 202 Accepted -- deletion is scheduled, not immediate")
    void schedulesAccountDeletion() throws Exception {
        User deleted = user;
        deleted.setDeletedAt(new Date());
        deleted.setScheduledDeletionAt(daysFromNow(30));
        deleted.setActive(false);

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(userService.softDeleteUser(user)).thenReturn(deleted);

        mockMvc.perform(delete("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoReactivationEnabled\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Account has been marked for deletion"))
                .andExpect(jsonPath("$.gracePeriodDays").value(30))
                .andExpect(jsonPath("$.deletedAt").exists())
                .andExpect(jsonPath("$.scheduledDeletionAt").exists());

        // A reactivation token is minted and stored so the deletion email has something to link to.
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("DELETE /users/me works with no body at all")
    void schedulesDeletionWithoutBody() throws Exception {
        user.setDeletedAt(new Date());
        user.setScheduledDeletionAt(daysFromNow(30));

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(userService.softDeleteUser(user)).thenReturn(user);

        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /users/reactivate restores an account inside the grace period")
    void reactivatesAccount() throws Exception {
        VerificationToken token = new VerificationToken();
        token.setToken("reactivation-token");
        token.setUser(user);
        token.setExpiryDate(daysFromNow(30));

        when(verificationTokenRepository.findByToken("reactivation-token")).thenReturn(Optional.of(token));
        when(userService.isWithinGracePeriod(user)).thenReturn(true);

        mockMvc.perform(post("/users/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactivationToken\":\"reactivation-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account successfully reactivated"));

        verify(userService).reactivateUser(user);
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    @DisplayName("POST /users/reactivate answers 400 for an unknown token")
    void rejectsUnknownReactivationToken() throws Exception {
        when(verificationTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactivationToken\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account reactivation failed"));

        verify(userService, never()).reactivateUser(any());
    }

    @Test
    @DisplayName("POST /users/reactivate answers 400 once the grace period has lapsed")
    void rejectsExpiredGracePeriod() throws Exception {
        VerificationToken token = new VerificationToken();
        token.setToken("stale");
        token.setUser(user);
        token.setExpiryDate(daysFromNow(1));

        when(verificationTokenRepository.findByToken("stale")).thenReturn(Optional.of(token));
        when(userService.isWithinGracePeriod(user)).thenReturn(false);

        mockMvc.perform(post("/users/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactivationToken\":\"stale\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Grace period has expired. Account cannot be reactivated."));

        verify(userService, never()).reactivateUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /users/admin/users/{id} soft-deletes by default")
    void adminSoftDeletesByDefault() throws Exception {
        when(userService.getUserById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/admin/users/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("User account marked for deletion"));

        verify(userService).softDeleteUser(user);
        verify(userService, never()).permanentlyDeleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /users/admin/users/{id} hard-deletes when forceDelete is set")
    void adminForceDeletes() throws Exception {
        when(userService.getUserById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forceDelete\":true}"))
                .andExpect(status().isOk())
                .andExpect(content().string("User permanently deleted"));

        verify(userService).permanentlyDeleteUser(user);
        verify(userService, never()).softDeleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /users/admin/users/{id} answers 404 for an unknown user")
    void adminDeleteReportsMissingUser() throws Exception {
        when(userService.getUserById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/users/admin/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /users/admin/users/{id}/extend-deletion pushes the deadline out")
    void adminExtendsGracePeriod() throws Exception {
        user.setDeletedAt(new Date());
        user.setScheduledDeletionAt(daysFromNow(10));
        when(userService.getUserById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/users/admin/users/2/extend-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extensionDays\":15}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deletion grace period extended by 15 days"));

        verify(userService).extendDeletionGracePeriod(user, 15);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /users/admin/users/{id}/extend-deletion rejects a non-positive extension")
    void adminRejectsNonPositiveExtension() throws Exception {
        mockMvc.perform(put("/users/admin/users/2/extend-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extensionDays\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Extension days must be greater than 0"));

        verify(userService, never()).extendDeletionGracePeriod(any(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /users/admin/users/{id}/extend-deletion rejects a user who is not scheduled")
    void adminRejectsUnscheduledUser() throws Exception {
        when(userService.getUserById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/users/admin/users/2/extend-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extensionDays\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User is not scheduled for deletion"));

        verify(userService, never()).extendDeletionGracePeriod(any(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /users/admin/users/scheduled-deletion lists the pending accounts")
    void adminListsScheduledDeletions() throws Exception {
        when(userService.getUsersScheduledForDeletion()).thenReturn(List.of(user));

        mockMvc.perform(get("/users/admin/users/scheduled-deletion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /users/admin/users/scheduled-deletion returns an empty list when nothing is due")
    void adminListsNothingWhenQueueEmpty() throws Exception {
        when(userService.getUsersScheduledForDeletion()).thenReturn(List.of());

        mockMvc.perform(get("/users/admin/users/scheduled-deletion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(userService).getUsersScheduledForDeletion();
        verify(userService, never()).getUserById(anyLong());
    }
}

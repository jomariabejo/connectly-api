package com.jomariabejo.connectly_api.tenant_api.controller;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantMemberDto;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantMembersResponseDto;
import com.jomariabejo.connectly_api.tenant_api.dto.UpdateTenantMemberRoleRequest;
import com.jomariabejo.connectly_api.tenant_api.service.TenantMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenants")
public class TenantMemberController {
    private final TenantMemberService tenantMemberService;
    private final AuthenticationService authenticationService;

    public TenantMemberController(
            TenantMemberService tenantMemberService,
            AuthenticationService authenticationService) {
        this.tenantMemberService = tenantMemberService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/members")
    public ResponseEntity<TenantMembersResponseDto> listMembers() {
        return ResponseEntity.ok(tenantMemberService.listMembersAndInvites());
    }

    @PatchMapping("/members/{userId}")
    public ResponseEntity<TenantMemberDto> updateMemberRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateTenantMemberRoleRequest request) {
        return ResponseEntity.ok(tenantMemberService.updateMemberRole(userId, request));
    }

    @DeleteMapping("/members/{userId}")
    public ResponseEntity<Void> deactivateMember(@PathVariable Long userId) {
        User actor = authenticationService.getAuthenticatedUser();
        tenantMemberService.deactivateMember(userId, actor);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(@PathVariable Long inviteId) {
        tenantMemberService.revokeInvite(inviteId);
        return ResponseEntity.noContent().build();
    }
}

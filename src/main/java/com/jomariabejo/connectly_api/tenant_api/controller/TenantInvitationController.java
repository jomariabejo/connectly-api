package com.jomariabejo.connectly_api.tenant_api.controller;

import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.CreateInviteRequest;
import com.jomariabejo.connectly_api.tenant_api.dto.InviteResponseDto;
import com.jomariabejo.connectly_api.tenant_api.service.TenantInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants")
public class TenantInvitationController {
    private final TenantInvitationService invitationService;
    private final AuthenticationService authenticationService;

    public TenantInvitationController(
            TenantInvitationService invitationService,
            AuthenticationService authenticationService) {
        this.invitationService = invitationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/invites")
    public ResponseEntity<InviteResponseDto> createInvite(@Valid @RequestBody CreateInviteRequest request) {
        User admin = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.createInvite(request, admin));
    }

    @GetMapping("/invites")
    public ResponseEntity<List<InviteResponseDto>> listInvites() {
        return ResponseEntity.ok(invitationService.listPendingInvites());
    }
}

package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.common.FrontendUrlBuilder;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.EmailService;
import com.jomariabejo.connectly_api.service.RateLimitingService;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.dto.CreateInviteRequest;
import com.jomariabejo.connectly_api.tenant_api.dto.InvitePreviewDto;
import com.jomariabejo.connectly_api.tenant_api.dto.InviteResponseDto;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantInvitation;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.exception.InvalidInvitationException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantInvitationRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TenantInvitationService {
    private final TenantInvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final TenantContextService tenantContextService;
    private final EmailService emailService;
    private final FrontendUrlBuilder frontendUrlBuilder;
    private final RateLimitingService rateLimitingService;

    @Value("${security.invitation.expiry-days:7}")
    private int invitationExpiryDays;

    public TenantInvitationService(
            TenantInvitationRepository invitationRepository,
            TenantRepository tenantRepository,
            TenantContextService tenantContextService,
            EmailService emailService,
            FrontendUrlBuilder frontendUrlBuilder,
            RateLimitingService rateLimitingService) {
        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.tenantContextService = tenantContextService;
        this.emailService = emailService;
        this.frontendUrlBuilder = frontendUrlBuilder;
        this.rateLimitingService = rateLimitingService;
    }

    @Transactional(readOnly = true)
    public InvitePreviewDto previewInvite(String token) {
        return invitationRepository.findByToken(token)
                .map(this::toPreview)
                .orElse(InvitePreviewDto.builder()
                        .valid(false)
                        .message("Invitation not found")
                        .build());
    }

    @Transactional
    public InviteResponseDto createInvite(CreateInviteRequest request, User admin) {
        requireInviteAdmin();
        Long tenantId = tenantContextService.requireTenantId();
        String rateKey = tenantId + ":" + request.getEmail().trim().toLowerCase();
        if (rateLimitingService.isRateLimited(rateKey, "tenant_invite_create")) {
            throw new InvalidInvitationException("Too many invite requests. Please try again later.");
        }
        rateLimitingService.recordAttempt(rateKey, "tenant_invite_create");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        validateInviteRole(request.getRole());

        String email = request.getEmail().trim().toLowerCase();
        if (invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(tenantId, email)) {
            throw new InvalidInvitationException("A pending invitation already exists for this email");
        }

        TenantInvitation invitation = TenantInvitation.builder()
                .tenant(tenant)
                .email(email)
                .tenantRole(request.getRole())
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(invitationExpiryDays))
                .createdBy(admin)
                .build();

        TenantInvitation saved = invitationRepository.save(invitation);
        String inviteLink = frontendUrlBuilder.inviteRegisterUrl(saved.getToken());
        emailService.sendTenantInviteEmail(email, tenant.getName(), request.getRole().name(), inviteLink);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InviteResponseDto> listPendingInvites() {
        Long tenantId = tenantContextService.requireTenantId();
        return invitationRepository.findByTenantIdAndAcceptedAtIsNullOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TenantInvitation consumeInvite(String token, String signupEmail) {
        TenantInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidInvitationException("Invalid invitation token"));

        if (invitation.isAccepted()) {
            throw new InvalidInvitationException("Invitation has already been used");
        }
        if (invitation.isExpired()) {
            throw new InvalidInvitationException("Invitation has expired");
        }
        if (!invitation.getEmail().equalsIgnoreCase(signupEmail.trim())) {
            throw new InvalidInvitationException("Email must match the invited address");
        }

        invitation.setAcceptedAt(LocalDateTime.now());
        return invitationRepository.save(invitation);
    }

    private void validateInviteRole(TenantRole role) {
        if (role != TenantRole.CUSTOMER && role != TenantRole.EMPLOYEE) {
            throw new InvalidInvitationException("Invites can only be created for CUSTOMER or EMPLOYEE roles");
        }
    }

    private InvitePreviewDto toPreview(TenantInvitation inv) {
        if (inv.isAccepted()) {
            return InvitePreviewDto.builder()
                    .valid(false)
                    .message("Invitation already accepted")
                    .build();
        }
        if (inv.isExpired()) {
            return InvitePreviewDto.builder()
                    .valid(false)
                    .message("Invitation has expired")
                    .build();
        }
        Tenant tenant = inv.getTenant();
        return InvitePreviewDto.builder()
                .valid(true)
                .tenantName(tenant.getName())
                .tenantSlug(tenant.getSlug())
                .role(inv.getTenantRole())
                .email(inv.getEmail())
                .message("Valid invitation")
                .build();
    }

    private InviteResponseDto toResponse(TenantInvitation inv) {
        return InviteResponseDto.builder()
                .id(inv.getId())
                .email(inv.getEmail())
                .role(inv.getTenantRole())
                .expiresAt(inv.getExpiresAt())
                .createdAt(inv.getCreatedAt())
                .build();
    }

    private void requireInviteAdmin() {
        TenantRole role = TenantContext.getTenantRole();
        if (role != TenantRole.OWNER && role != TenantRole.ADMIN && role != TenantRole.STAFF) {
            throw new TenantAccessDeniedException("Insufficient permissions to manage invitations");
        }
    }
}

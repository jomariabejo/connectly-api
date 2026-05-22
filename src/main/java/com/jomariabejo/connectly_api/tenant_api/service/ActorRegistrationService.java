package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.common.FrontendUrlBuilder;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.exception.InvalidCredentialsException;
import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.service.EmailService;
import com.jomariabejo.connectly_api.service.RateLimitingService;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import com.jomariabejo.connectly_api.tenant_api.dto.PublicTenantDto;
import com.jomariabejo.connectly_api.tenant_api.dto.RegisterCustomerRequest;
import com.jomariabejo.connectly_api.tenant_api.dto.RegisterInviteRequest;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantInvitation;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantStatus;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantUser;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantRegistrationException;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActorRegistrationService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final FrontendUrlBuilder frontendUrlBuilder;
    private final TenantInvitationService invitationService;
    private final RateLimitingService rateLimitingService;

    public ActorRegistrationService(
            TenantRepository tenantRepository,
            TenantUserRepository tenantUserRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService verificationTokenService,
            EmailService emailService,
            FrontendUrlBuilder frontendUrlBuilder,
            TenantInvitationService invitationService,
            RateLimitingService rateLimitingService) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.frontendUrlBuilder = frontendUrlBuilder;
        this.invitationService = invitationService;
        this.rateLimitingService = rateLimitingService;
    }

    @Transactional(readOnly = true)
    public PublicTenantDto resolvePublicTenant(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new TenantNotFoundException("Organization not found"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantRegistrationException("Organization is not accepting signups");
        }
        return PublicTenantDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .build();
    }

    @Transactional
    public User registerCustomer(RegisterCustomerRequest request) {
        RegisterUserDto dto = request.getUser();
        enforceRateLimit(dto.getEmail(), "register_customer");

        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug().trim())
                .orElseThrow(() -> new TenantNotFoundException("Organization not found"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantRegistrationException("Organization is not accepting signups");
        }

        return registerWithTenantMembership(dto, tenant, TenantRole.CUSTOMER);
    }

    @Transactional
    public User registerViaInvite(RegisterInviteRequest request) {
        RegisterUserDto dto = request.getUser();
        enforceRateLimit(dto.getEmail(), "register_invite");

        TenantInvitation invitation = invitationService.consumeInvite(
                request.getInviteToken(),
                dto.getEmail()
        );
        Tenant tenant = invitation.getTenant();
        return registerWithTenantMembership(dto, tenant, invitation.getTenantRole());
    }

    private User registerWithTenantMembership(RegisterUserDto dto, Tenant tenant, TenantRole role) {
        String email = dto.getEmail().trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmailNormalized(email);

        if (existing.isPresent()) {
            User user = existing.get();
            if (tenantUserRepository.existsByTenantIdAndUserId(tenant.getId(), user.getId())) {
                throw new TenantRegistrationException("You are already a member of this organization");
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new InvalidCredentialsException("Invalid password for existing account");
            }
            attachTenantMembership(user, tenant, role);
            return user;
        }

        if (userRepository.existsAnyByUsername(dto.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(email);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(false);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        Date expiryDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
        user.setExpiryDate(expiryDate);
        user = userRepository.save(user);

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user, token);
        emailService.sendVerificationEmail(
                user.getEmail(),
                frontendUrlBuilder.verifyEmailUrl(token),
                verificationToken.getOtp(),
                frontendUrlBuilder.checkEmailUrl()
        );

        attachTenantMembership(user, tenant, role);
        return user;
    }

    private void attachTenantMembership(User user, Tenant tenant, TenantRole role) {
        TenantUser membership = TenantUser.builder()
                .tenant(tenant)
                .user(user)
                .tenantRole(role)
                .active(true)
                .build();
        tenantUserRepository.save(membership);
    }

    private void enforceRateLimit(String email, String action) {
        String key = email.trim().toLowerCase();
        if (rateLimitingService.isRateLimited(key, action)) {
            throw new TenantRegistrationException("Too many registration attempts. Please try again later.");
        }
        rateLimitingService.recordAttempt(key, action);
    }
}

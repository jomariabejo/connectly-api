package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.InviteResponseDto;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantMemberDto;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantMembersResponseDto;
import com.jomariabejo.connectly_api.tenant_api.dto.UpdateTenantMemberRoleRequest;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantUser;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantInvitationRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantUserRepository;
import com.jomariabejo.connectly_api.tenant_api.support.TenantRolePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantMemberService {
    private final TenantUserRepository tenantUserRepository;
    private final TenantInvitationRepository invitationRepository;
    private final TenantContextService tenantContextService;
    private final TenantInvitationService invitationService;

    public TenantMemberService(
            TenantUserRepository tenantUserRepository,
            TenantInvitationRepository invitationRepository,
            TenantContextService tenantContextService,
            TenantInvitationService invitationService) {
        this.tenantUserRepository = tenantUserRepository;
        this.invitationRepository = invitationRepository;
        this.tenantContextService = tenantContextService;
        this.invitationService = invitationService;
    }

    @Transactional(readOnly = true)
    public TenantMembersResponseDto listMembersAndInvites() {
        requireTeamManagement();
        Long tenantId = tenantContextService.requireTenantId();
        List<TenantMemberDto> members = tenantUserRepository
                .findByTenantIdWithUserAndActiveTrue(tenantId)
                .stream()
                .map(this::toMemberDto)
                .toList();
        List<InviteResponseDto> invites = invitationService.listPendingInvites();
        return TenantMembersResponseDto.builder()
                .members(members)
                .pendingInvites(invites)
                .build();
    }

    @Transactional
    public TenantMemberDto updateMemberRole(Long userId, UpdateTenantMemberRoleRequest request) {
        requireRoleChangePermission();
        Long tenantId = tenantContextService.requireTenantId();
        TenantUser membership = tenantUserRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, userId)
                .orElseThrow(() -> new TenantNotFoundException("Member not found"));

        TenantRole newRole = request.getRole();
        if (newRole == TenantRole.OWNER) {
            throw new IllegalArgumentException("Use store creation to assign ownership; OWNER cannot be assigned via promotion");
        }

        TenantRole currentRole = membership.getTenantRole();
        if (currentRole == TenantRole.OWNER && newRole != TenantRole.OWNER) {
            long ownerCount = tenantUserRepository.countByTenantIdAndTenantRoleAndActiveTrue(
                    tenantId, TenantRole.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last owner of this store");
            }
        }

        membership.setTenantRole(newRole);
        return toMemberDto(tenantUserRepository.save(membership));
    }

    @Transactional
    public void deactivateMember(Long userId, User actor) {
        requireRoleChangePermission();
        Long tenantId = tenantContextService.requireTenantId();
        if (actor.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot remove yourself; transfer ownership first");
        }

        TenantUser membership = tenantUserRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, userId)
                .orElseThrow(() -> new TenantNotFoundException("Member not found"));

        if (membership.getTenantRole() == TenantRole.OWNER) {
            long ownerCount = tenantUserRepository.countByTenantIdAndTenantRoleAndActiveTrue(
                    tenantId, TenantRole.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last owner of this store");
            }
        }

        membership.setActive(false);
        tenantUserRepository.save(membership);
    }

    @Transactional
    public void revokeInvite(Long inviteId) {
        requireTeamManagement();
        Long tenantId = tenantContextService.requireTenantId();
        invitationRepository.findByIdAndTenantId(inviteId, tenantId)
                .filter(inv -> inv.getAcceptedAt() == null)
                .ifPresentOrElse(
                        invitationRepository::delete,
                        () -> {
                            throw new TenantNotFoundException("Pending invitation not found");
                        });
    }

    private TenantMemberDto toMemberDto(TenantUser membership) {
        User user = membership.getUser();
        return TenantMemberDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(membership.getTenantRole())
                .active(membership.getActive())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    private void requireTeamManagement() {
        TenantRole role = tenantContextService.requireTenantRole();
        if (!TenantRolePolicy.canManageTeam(role)) {
            throw new TenantAccessDeniedException("Insufficient permissions to view team members");
        }
    }

    private void requireRoleChangePermission() {
        TenantRole role = tenantContextService.requireTenantRole();
        if (!TenantRolePolicy.canChangeMemberRoles(role)) {
            throw new TenantAccessDeniedException("Only the store owner can change member roles");
        }
    }
}

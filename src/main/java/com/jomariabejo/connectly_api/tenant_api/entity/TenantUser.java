package com.jomariabejo.connectly_api.tenant_api.entity;

import com.jomariabejo.connectly_api.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_role", nullable = false, length = 30)
    @Builder.Default
    private TenantRole tenantRole = TenantRole.MANAGER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}

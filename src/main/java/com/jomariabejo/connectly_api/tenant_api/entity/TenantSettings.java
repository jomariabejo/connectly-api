package com.jomariabejo.connectly_api.tenant_api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "sms_notifications_enabled")
    @Builder.Default
    private Boolean smsNotificationsEnabled = true;

    @Column(name = "email_notifications_enabled")
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "business_type_config")
    private Map<String, Object> businessTypeConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

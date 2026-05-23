package com.jomariabejo.connectly_api.tenant_api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Business type templates for industry-specific configurations
 * Stores default settings and sample data for different business types
 */
@Entity
@Table(name = "business_type_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessTypeTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String businessType;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String iconUrl;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> defaultSettings;

    @Column(columnDefinition = "TEXT")
    private String sampleDataSql;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.jomariabejo.connectly_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "app_user")
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(nullable = true, unique = true)
    private String verificationToken;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "deleted_at", nullable = true)
    private Date deletedAt;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "scheduled_deletion_at", nullable = true)
    private Date scheduledDeletionAt;

    @Column(name = "auto_reactivation_enabled")
    private Boolean autoReactivationEnabled = true;

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = false;
    }

    @Override
    public String getUsername() {
        return email;  // Email is used as the username
    }

    /**
     * The value stored in the {@code username} column.
     *
     * <p>Needed because the {@link org.springframework.security.core.userdetails.UserDetails}
     * override above returns the email, which stops Lombok generating an accessor for the field
     * itself -- leaving it otherwise unreadable from outside this class.
     */
    public String getUsernameField() {
        return this.username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : roles) {
            authorities.add(() -> "ROLE_" + role.getName());
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }

    public boolean isAutoReactivationEnabled() {
        return autoReactivationEnabled;
    }

    public void setAutoReactivationEnabled(boolean autoReactivationEnabled) {
        this.autoReactivationEnabled = autoReactivationEnabled;
    }
}

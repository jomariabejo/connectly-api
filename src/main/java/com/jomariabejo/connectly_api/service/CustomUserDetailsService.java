package com.jomariabejo.connectly_api.service;


import com.jomariabejo.connectly_api.exception.AccountDeletionScheduledException;
import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service("userDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    
    @Lazy
    @Autowired(required = false)
    private UserService userService;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = normalizeLoginIdentifier(username);
        User user = userRepository.findByEmailNormalized(normalized)
                .or(() -> userRepository.findByUsername(normalized))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
        
        // Check if user is deleted
        if (user.getDeletedAt() != null) {
            // Check if user is within grace period and auto-reactivation is enabled
            if (userService != null && user.isAutoReactivationEnabled() && userService.isWithinGracePeriod(user)) {
                // Auto-reactivate the user
                user = userService.reactivateUser(user);
                return user;
            } else {
                // User is deleted and either outside grace period or auto-reactivation is disabled
                throw new AccountDeletionScheduledException(
                        "Account has been marked for deletion",
                        java.time.Instant.ofEpochMilli(user.getDeletedAt().getTime())
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                        java.time.Instant.ofEpochMilli(user.getScheduledDeletionAt().getTime())
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                );
            }
        }
        
        return user;
    }

    public static String normalizeLoginIdentifier(String identifier) {
        if (identifier == null) {
            return "";
        }
        return identifier.trim().toLowerCase();
    }

    private Set<GrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());
    }
}


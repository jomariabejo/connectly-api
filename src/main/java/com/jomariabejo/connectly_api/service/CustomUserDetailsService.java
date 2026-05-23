package com.jomariabejo.connectly_api.service;


import com.jomariabejo.connectly_api.exception.AccountDeletionScheduledException;
import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
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

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = normalizeLoginIdentifier(username);
        User user = userRepository.findByEmailNormalized(normalized)
                .or(() -> userRepository.findByUsername(normalized))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
        
        if (user.getDeletedAt() != null) {
            throw new AccountDeletionScheduledException(
                    "Account has been marked for deletion. Use the email reactivation link to restore access.",
                    java.time.Instant.ofEpochMilli(user.getDeletedAt().getTime())
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                    java.time.Instant.ofEpochMilli(user.getScheduledDeletionAt().getTime())
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            );
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

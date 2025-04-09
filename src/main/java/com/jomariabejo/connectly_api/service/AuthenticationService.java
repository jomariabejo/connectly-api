package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(RegisterUserDto input) {
        User user = new User();
        user.setUsername(input.getUsername());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setEmail(input.getEmail());

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        System.out.println("Authenticating user with email: " + input.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        User authenticatedUser = (User) authentication.getPrincipal();
        System.out.println("Successfully authenticated user: " + authenticatedUser.getEmail());

        return authenticatedUser;
    }
}
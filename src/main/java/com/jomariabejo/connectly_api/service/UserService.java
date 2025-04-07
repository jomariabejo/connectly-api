package com.jomariabejo.connectly_api.service;


import com.jomariabejo.connectly_api.dto.RegisterRequest;
import com.jomariabejo.connectly_api.dto.AuthResponse;
import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new AuthResponse("Username is already taken", false);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new AuthResponse("Email is already in use", false);
        }

        // Create new user
        User user = new User(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail(),
                Role.USER  // Default role for new users
        );

        // Save user to database
        userRepository.save(user);

        return new AuthResponse("User registered successfully", true);
    }
}

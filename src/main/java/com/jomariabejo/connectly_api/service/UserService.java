package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.AuthResponse;
import com.jomariabejo.connectly_api.dto.LoginRequest;
import com.jomariabejo.connectly_api.dto.RegisterRequest;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user by encrypting the password and saving the user to the database.
     *
     * @param registerRequest the DTO containing user registration data.
     * @return AuthResponse with success or failure message.
     */
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        // Check if the username or email already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new AuthResponse(false, "Username already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new AuthResponse(false, "Email already in use");
        }

        // Encrypt the password
        String encryptedPassword = passwordEncoder.encode(registerRequest.getPassword());

        // Create a new user
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(encryptedPassword);
        newUser.setEmail(registerRequest.getEmail());
//        newUser.setRole(registerRequest.getRole());  // Assuming the role is passed in the registration request
//        newUser.setEnabled(true);  // User is enabled by default

        try {
            // Save the new user to the database
            userRepository.save(newUser);
        } catch (Exception e) {
            return new AuthResponse(false, "Error saving user: " + e.getMessage());
        }

        // Return success response
        return new AuthResponse(true, "Registration successful");
    }

    /**
     * Authenticates a user, checking their credentials and generating a JWT token.
     *
     * @param loginRequest the DTO containing login credentials (username/email and password).
     * @return AuthResponse with success status and JWT token or error message.
     */
    public AuthResponse loginUser(LoginRequest loginRequest) {
        // Find user by username or email
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail());

        // If user not found or password doesn't match, return failure response
        return userOptional
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user);
                    return new AuthResponse("Login successful", true, token);
                })
                .orElseGet(() -> new AuthResponse(false, "Invalid credentials"));
    }
}

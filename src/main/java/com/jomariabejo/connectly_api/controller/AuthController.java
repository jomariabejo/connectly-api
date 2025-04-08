package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.AuthResponse;
import com.jomariabejo.connectly_api.dto.LoginRequest;
import com.jomariabejo.connectly_api.dto.RegisterRequest;
import com.jomariabejo.connectly_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Endpoint to register a new user.
     *
     * @param registerRequest the DTO containing the user registration data.
     * @return ResponseEntity with AuthResponse.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        AuthResponse response = userService.registerUser(registerRequest);
        return ResponseEntity.status(response.isSuccess() ? 201 : 400).body(response);
    }

    /**
     * Endpoint to authenticate a user and return a JWT token.
     *
     * @param loginRequest the DTO containing the login credentials.
     * @return ResponseEntity with AuthResponse containing JWT token if successful.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse response = userService.loginUser(loginRequest);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response); // Unauthorized if login fails
    }
}

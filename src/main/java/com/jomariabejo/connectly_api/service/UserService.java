package com.jomariabejo.connectly_api.service;//package com.jomariabejo.connectly_api.service;
//
//import com.jomariabejo.connectly_api.dto.AuthResponse;
//import com.jomariabejo.connectly_api.dto.LoginRequest;
//import com.jomariabejo.connectly_api.dto.RegisterRequest;
//import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
//import com.jomariabejo.connectly_api.exception.InvalidCredentialsException;
//import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
//import com.jomariabejo.connectly_api.model.User;
//import com.jomariabejo.connectly_api.repository.UserRepository;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.java.Log;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.security.Key;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//@Log
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final BCryptPasswordEncoder passwordEncoder;
//
//    @Value("${jwt.secret}")
//    private String secretKey;
//
//    @Value("${jwt.expiration-time}")
//    private long jwtExpiration;
//
//    @Autowired
//    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    public AuthResponse registerUser(RegisterRequest registerRequest) {
//        if (userRepository.existsByUsername(registerRequest.getUsername())) {
//            log.info("Attempt to register with an existing username: " + registerRequest.getUsername());
//            throw new UserAlreadyExistsException("Username already taken");
//        }
//
//        if (userRepository.existsByEmail(registerRequest.getEmail())) {
//            log.info("Attempt to register with an existing email: " + registerRequest.getEmail());
//            throw new EmailAlreadyInUseException("Email already in use");
//        }
//
//        String encryptedPassword = passwordEncoder.encode(registerRequest.getPassword());
//
//        User newUser = new User();
//        newUser.setUsername(registerRequest.getUsername());
//        newUser.setPassword(encryptedPassword);
//        newUser.setEmail(registerRequest.getEmail());
//
//        try {
//            userRepository.save(newUser);
//            log.info("Successfully registered user: " + registerRequest.getUsername());
//        } catch (Exception e) {
//            log.severe("Error saving user: " + e.getMessage());
//            throw new RuntimeException("Error saving user: " + e.getMessage());
//        }
//
//        return new AuthResponse(true, "Registration successful");
//    }
//
//    public AuthResponse authenticateUser(LoginRequest loginRequest) {
//        // Retrieve user by username or email
//        Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail());
//
//        // Check if user exists
//        if (userOpt.isEmpty()) {
//            log.warning("Invalid login attempt for username/email: " + loginRequest.getUsernameOrEmail());
//            throw new InvalidCredentialsException("Invalid username or password");
//        }
//
//        User user = userOpt.get();
//
//        // Validate password
//        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
//            log.warning("Invalid password attempt for user: " + loginRequest.getUsernameOrEmail());
//            throw new InvalidCredentialsException("Invalid username or password");
//        }
//
//        // Generate JWT token if authentication is successful
//        String token = generateJwtToken(user);
//        return new AuthResponse("Login successful", true, token);
//    }
//
//    public String generateJwtToken(User user) {
//        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
//
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("sub", user.getUsername());
//        claims.put("role", user.getRole());
//        claims.put("iat", new Date());
//
//        Date expirationDate = new Date(System.currentTimeMillis() + jwtExpiration);
//
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(user.getUsername())
//                .setExpiration(expirationDate)
//                .signWith(key)
//                .compact();
//    }
//}


import ch.qos.logback.core.model.Model;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.sql.Timestamp;
import java.util.*;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final UserDetailsService userDetailsService;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       @Qualifier("userDetailsService") UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userDetailsService = userDetailsService;
    }

    public List<User> allUsers() {
        return new ArrayList<>(userRepository.findAll());
    }

    public void confirmRegistration(User user, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(calculateExpiryDate(60 * 24)); // 24 hours
        tokenRepository.save(verificationToken);
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }
}
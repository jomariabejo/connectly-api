package com.jomariabejo.connectly_api.service;//package com.jomariabejo.connectly_api.service;
//
//import com.jomariabejo.connectly_api.dto.AuthResponse;
//import com.jomariabejo.connectly_api.dto.LoginRequest;
//import com.jomariabejo.connectly_api.dto.RegisterRequest;
//import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
//import com.jomariabejo.connectly_api.exception.InvalidCredentialsException;
//import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
//import com.jomariabejo.connectly_api.model.User;
//import com.jomariabejo.connectly_api.model.Post;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.java.Log;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.CommentRepository;
import com.jomariabejo.connectly_api.repository.LikeRespository;
import com.jomariabejo.connectly_api.repository.PasswordResetTokenRepository;
import com.jomariabejo.connectly_api.repository.PostRepository;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final UserDetailsService userDetailsService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRespository likeRespository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       @Qualifier("userDetailsService") UserDetailsService userDetailsService,
                       PostRepository postRepository,
                       CommentRepository commentRepository,
                       LikeRespository likeRespository,
                       PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userDetailsService = userDetailsService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRespository = likeRespository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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

    // Pagination methods
    public PaginationDto<UserResponseDto> getAllUsersPaginated(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        return mapPageToDto(usersPage);
    }

    public PaginationDto<UserResponseDto> getAllUsersWithFilters(UserFilterDto filterDto, Pageable pageable) {
        Page<User> usersPage = userRepository.findWithFilters(
                filterDto.getUsername(),
                filterDto.getEmail(),
                filterDto.getFirstName(),
                filterDto.getLastName(),
                pageable
        );
        return mapPageToDto(usersPage);
    }

    private PaginationDto<UserResponseDto> mapPageToDto(Page<User> page) {
        List<UserResponseDto> content = page.getContent().stream()
                .map(UserResponseDto::from)
                .toList();
        return new PaginationDto<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Soft-delete a user account (non-permanent, 30-day grace period before permanent deletion).
     * Sets deletion timestamps and schedules permanent deletion after 30 days.
     *
     * @param user The user to soft-delete
     * @return Updated user with deletion timestamps set
     */
    public User softDeleteUser(User user) {
        Date deletedAt = new Date();
        Date scheduledDeletionAt = calculateScheduledDeletionDate(30); // 30 days grace period

        user.setDeletedAt(deletedAt);
        user.setScheduledDeletionAt(scheduledDeletionAt);
        user.setActive(false);

        return userRepository.save(user);
    }

    /**
     * Reactivate a soft-deleted user account within the grace period.
     * Clears deletion timestamps and resets active status.
     *
     * @param user The user to reactivate
     * @return Updated user with deletion timestamps cleared
     */
    public User reactivateUser(User user) {
        user.setDeletedAt(null);
        user.setScheduledDeletionAt(null);
        user.setActive(true);

        return userRepository.save(user);
    }

    /**
     * Permanently delete a user account (hard delete).
     * This removes all user data and related records. Should normally only be called after grace period expires.
     *
     * @param user The user to permanently delete
     */
    @Transactional
    public void permanentlyDeleteUser(User user) {
        // Every foreign key pointing at app_user is NO ACTION in the generated schema, so the
        // dependants have to go first. Without this the delete threw a constraint violation, the
        // scheduled task swallowed it, and any account with a single post, comment or like stayed
        // soft-deleted forever while its owner had been told the data was gone.
        likeRespository.deleteAllByUser(user);
        commentRepository.deleteAllByUser(user);

        // Comments and likes left by *other* people on this user's posts block the post delete.
        List<Post> posts = postRepository.findByCreatedById(user.getId());
        for (Post post : posts) {
            likeRespository.deleteAllByPost(post);
            commentRepository.deleteAllByPost(post);
            postRepository.delete(post);
        }

        tokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteAllByUser(user);

        user.getRoles().clear();
        userRepository.save(user);

        userRepository.delete(user);
        logger.info("Permanently deleted user {} and {} post(s)", user.getEmail(), posts.size());
    }

    /**
     * Extend the deletion grace period for a user by a specified number of days.
     * Can only be called by admins (authorization should be checked at controller level).
     *
     * @param user The deleted user
     * @param additionalDays Number of days to extend the grace period by
     * @return Updated user with extended deletion date
     */
    public User extendDeletionGracePeriod(User user, int additionalDays) {
        if (user.getScheduledDeletionAt() == null) {
            throw new IllegalStateException("User is not scheduled for deletion");
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(user.getScheduledDeletionAt());
        cal.add(Calendar.DAY_OF_MONTH, additionalDays);

        user.setScheduledDeletionAt(new Date(cal.getTimeInMillis()));
        return userRepository.save(user);
    }

    /**
     * Check for users scheduled for permanent deletion and delete them if the scheduled time has passed.
     * This is a scheduled task that runs periodically (e.g., daily).
     *
     * @return Count of users permanently deleted
     */
    @Transactional
    public int checkAndDeleteScheduledUsers() {
        List<User> scheduledForDeletion = userRepository.findUsersScheduledForDeletion();
        
        for (User user : scheduledForDeletion) {
            permanentlyDeleteUser(user);
        }

        return scheduledForDeletion.size();
    }

    /**
     * Check if a user is within the deletion grace period.
     *
     * @param user The user to check
     * @return true if user is deleted and within grace period, false otherwise
     */
    public boolean isWithinGracePeriod(User user) {
        if (user.getDeletedAt() == null) {
            return false; // Not deleted
        }
        if (user.getScheduledDeletionAt() == null) {
            return false; // Deleted but no schedule recorded — treat as outside the grace period
        }

        return user.getScheduledDeletionAt().after(new Date());
    }

    /**
     * Calculate a date for scheduled deletion (grace period).
     *
     * @param gracePeriodDays Number of days in the grace period
     * @return Date when the grace period expires
     */
    private Date calculateScheduledDeletionDate(int gracePeriodDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, gracePeriodDays);
        return new Date(cal.getTimeInMillis());
    }

    /**
     * Get a user by ID (only active users).
     *
     * @param id The user ID
     * @return Optional containing the user if found and not deleted
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findActiveUserById(id);
    }

    /**
     * Looks a user up regardless of soft-delete state.
     *
     * <p>The admin endpoints exist to manage accounts that are already scheduled for deletion, but
     * they resolved users through {@link #getUserById(Long)}, which filters {@code deletedAt IS NULL}.
     * A soft-deleted account was therefore invisible to them: force-delete answered 404, and
     * extend-deletion could never reach its "user is not scheduled for deletion" branch because a
     * scheduled user was never returned in the first place.
     */
    public Optional<User> getAnyUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get all users scheduled for permanent deletion.
     *
     * @return List of users with scheduled_deletion_at <= now
     */
    public List<User> getUsersScheduledForDeletion() {
        return userRepository.findUsersScheduledForDeletion();
    }
}
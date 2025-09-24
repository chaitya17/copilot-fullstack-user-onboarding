package com.useronboard.service.service;

import com.useronboard.service.dto.UserRegistrationRequest;
import com.useronboard.service.dto.UserResponse;
import com.useronboard.service.entity.User;
import com.useronboard.service.entity.UserAuditLog;
import com.useronboard.service.entity.UserStatus;
import com.useronboard.service.messaging.UserEventPublisher;
import com.useronboard.service.repository.UserAuditLogRepository;
import com.useronboard.service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User service implementing the core business logic
 * Handles user registration, status management, and admin operations
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                      UserAuditLogRepository auditLogRepository,
                      PasswordEncoder passwordEncoder,
                      UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Register a new user with PENDING status
     */
    public UserResponse registerUser(UserRegistrationRequest request) {
        logger.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user entity
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.PENDING);
        user.setRoles("USER");

        // Save user
        User savedUser = userRepository.save(user);

        // Create audit log
        UserAuditLog auditLog = UserAuditLog.userCreated(
            savedUser.getId(),
            "User registered via API"
        );
        auditLogRepository.save(auditLog);

        // Publish user registration event for async processing
        eventPublisher.publishUserRegisteredEvent(savedUser);

        logger.info("User registered successfully: {} with ID: {}", savedUser.getEmail(), savedUser.getId());

        return convertToResponse(savedUser);
    }

    /**
     * Authenticate user login
     */
    public Optional<User> authenticateUser(String email, String password) {
        logger.debug("Authenticating user: {}", email);

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check password
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                // Check if user is active
                if (user.isActive()) {
                    logger.info("User authenticated successfully: {}", email);
                    return Optional.of(user);
                } else {
                    logger.warn("User authentication failed - user not active: {} (status: {})", email, user.getStatus());
                    throw new IllegalStateException("User account is not active. Status: " + user.getStatus());
                }
            } else {
                logger.warn("User authentication failed - invalid password: {}", email);
            }
        } else {
            logger.warn("User authentication failed - user not found: {}", email);
        }

        return Optional.empty();
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(String userId) {
        return userRepository.findById(userId)
                .map(this::convertToResponse);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(this::convertToResponse);
    }

    /**
     * Get all pending users for admin review
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getPendingUsers() {
        return userRepository.findByStatusOrderByCreatedAtAsc(UserStatus.PENDING)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approve user by admin
     */
    public UserResponse approveUser(String userId, String adminId, String reason) {
        logger.info("Admin {} approving user: {}", adminId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isPending()) {
            throw new IllegalStateException("User is not in PENDING status. Current status: " + user.getStatus());
        }

        // Update user status
        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Create audit log
        UserAuditLog auditLog = UserAuditLog.userApproved(userId, adminId, reason);
        auditLogRepository.save(auditLog);

        // Publish user approved event for welcome email
        eventPublisher.publishUserApprovedEvent(savedUser, adminId);

        logger.info("User approved successfully: {} by admin: {}", userId, adminId);

        return convertToResponse(savedUser);
    }

    /**
     * Reject user by admin
     */
    public UserResponse rejectUser(String userId, String adminId, String reason) {
        logger.info("Admin {} rejecting user: {}", adminId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isPending()) {
            throw new IllegalStateException("User is not in PENDING status. Current status: " + user.getStatus());
        }

        // Update user status
        user.setStatus(UserStatus.REJECTED);
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Create audit log
        UserAuditLog auditLog = UserAuditLog.userRejected(userId, adminId, reason);
        auditLogRepository.save(auditLog);

        // Publish user rejected event
        eventPublisher.publishUserRejectedEvent(savedUser, adminId, reason);

        logger.info("User rejected successfully: {} by admin: {}", userId, adminId);

        return convertToResponse(savedUser);
    }

    /**
     * Get users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    /**
     * Get user statistics for dashboard
     */
    @Transactional(readOnly = true)
    public Object[] getUserStatistics() {
        return userRepository.getUserStatistics();
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRoles(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

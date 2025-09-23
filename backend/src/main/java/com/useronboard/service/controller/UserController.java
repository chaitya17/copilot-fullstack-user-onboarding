package com.useronboard.service.controller;

import com.useronboard.service.dto.ApiResponse;
import com.useronboard.service.dto.UserResponse;
import com.useronboard.service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * User controller handling user profile operations
 * Provides endpoints for authenticated users to manage their profiles
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current user profile
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            String userId = getCurrentUserId();

            Optional<UserResponse> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                logger.debug("Retrieved user profile for user: {}", userId);
                return ResponseEntity.ok(ApiResponse.success(userOpt.get()));
            } else {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving user profile", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to retrieve user profile"));
        }
    }

    /**
     * Get user profile by ID (admin or same user only)
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        try {
            String currentUserId = getCurrentUserId();

            // Users can only view their own profile unless they're admin
            if (!currentUserId.equals(userId) && !isCurrentUserAdmin()) {
                logger.warn("Unauthorized access attempt to user profile: {} by user: {}", userId, currentUserId);
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied"));
            }

            Optional<UserResponse> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                logger.debug("Retrieved user profile: {}", userId);
                return ResponseEntity.ok(ApiResponse.success(userOpt.get()));
            } else {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving user profile for user: {}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to retrieve user profile"));
        }
    }

    /**
     * Get current user ID from security context
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated");
    }

    /**
     * Check if current user has admin role
     */
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.getAuthorities().stream()
                   .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}

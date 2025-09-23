package com.useronboard.service.controller;

import com.useronboard.service.dto.AdminActionRequest;
import com.useronboard.service.dto.ApiResponse;
import com.useronboard.service.dto.UserResponse;
import com.useronboard.service.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for user management operations
 * Requires ADMIN role for all endpoints
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all pending users for approval
     * GET /api/v1/admin/users/pending
     */
    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingUsers() {
        try {
            List<UserResponse> pendingUsers = userService.getPendingUsers();

            logger.info("Retrieved {} pending users for admin review", pendingUsers.size());

            return ResponseEntity.ok(ApiResponse.success("Pending users retrieved successfully", pendingUsers));

        } catch (Exception e) {
            logger.error("Error retrieving pending users", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to retrieve pending users"));
        }
    }

    /**
     * Get all users with pagination
     * GET /api/v1/admin/users?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<UserResponse> users = userService.getUsers(pageable);

            logger.debug("Retrieved {} users (page {}, size {})", users.getNumberOfElements(), page, size);

            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));

        } catch (Exception e) {
            logger.error("Error retrieving users", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to retrieve users"));
        }
    }

    /**
     * Approve user
     * POST /api/v1/admin/users/{userId}/approve
     */
    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(
            @PathVariable String userId,
            @Valid @RequestBody AdminActionRequest request) {

        try {
            String adminId = getCurrentUserId();
            String reason = request.getReason() != null ? request.getReason() : "Approved by admin";

            logger.info("Admin {} approving user: {}", adminId, userId);

            UserResponse approvedUser = userService.approveUser(userId, adminId, reason);

            logger.info("User {} approved successfully by admin {}", userId, adminId);

            return ResponseEntity.ok(ApiResponse.success("User approved successfully", approvedUser));

        } catch (IllegalArgumentException e) {
            logger.warn("Failed to approve user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Failed to approve user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(400)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving user: {}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to approve user"));
        }
    }

    /**
     * Reject user
     * POST /api/v1/admin/users/{userId}/reject
     */
    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(
            @PathVariable String userId,
            @Valid @RequestBody AdminActionRequest request) {

        try {
            String adminId = getCurrentUserId();
            String reason = request.getReason() != null ? request.getReason() : "Rejected by admin";

            logger.info("Admin {} rejecting user: {}", adminId, userId);

            UserResponse rejectedUser = userService.rejectUser(userId, adminId, reason);

            logger.info("User {} rejected successfully by admin {}", userId, adminId);

            return ResponseEntity.ok(ApiResponse.success("User rejected successfully", rejectedUser));

        } catch (IllegalArgumentException e) {
            logger.warn("Failed to reject user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Failed to reject user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(400)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting user: {}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to reject user"));
        }
    }

    /**
     * Get user statistics
     * GET /api/v1/admin/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getUserStatistics() {
        try {
            Object[] stats = userService.getUserStatistics();

            // Convert to readable format
            java.util.Map<String, Object> statistics = new java.util.HashMap<>();
            if (stats != null && stats.length >= 3) {
                statistics.put("pending", stats[0] != null ? ((Number) stats[0]).longValue() : 0L);
                statistics.put("active", stats[1] != null ? ((Number) stats[1]).longValue() : 0L);
                statistics.put("rejected", stats[2] != null ? ((Number) stats[2]).longValue() : 0L);
            }

            logger.debug("Retrieved user statistics: {}", statistics);

            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));

        } catch (Exception e) {
            logger.error("Error retrieving user statistics", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to retrieve statistics"));
        }
    }

    /**
     * Get current admin user ID from security context
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("Admin not authenticated");
    }
}

package com.useronboard.service.service;

import com.useronboard.service.dto.AuthResponse;
import com.useronboard.service.dto.LoginRequest;
import com.useronboard.service.dto.UserResponse;
import com.useronboard.service.entity.RefreshToken;
import com.useronboard.service.entity.User;
import com.useronboard.service.repository.RefreshTokenRepository;
import com.useronboard.service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Authentication service handling JWT token operations
 * Manages login, logout, token refresh, and token revocation
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Value("${security.jwt.access-token-expiry:15m}")
    private String accessTokenExpiry;

    @Value("${security.jwt.refresh-token-expiry:7d}")
    private String refreshTokenExpiry;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserService userService, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Authenticate user and generate tokens
     */
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("User login attempt: {}", loginRequest.getEmail());

        // Authenticate user
        Optional<User> userOpt = userService.authenticateUser(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        );

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userOpt.get();

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Store refresh token in database
        storeRefreshToken(user.getId(), refreshToken);

        // Convert user to response
        UserResponse userResponse = new UserResponse(
            user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
            user.getPhone(), user.getRoles(), user.getStatus(),
            user.getCreatedAt(), user.getUpdatedAt()
        );

        logger.info("User logged in successfully: {}", user.getEmail());

        return new AuthResponse(accessToken, getExpiryInSeconds(accessTokenExpiry), userResponse);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshTokenValue) {
        logger.debug("Refreshing access token");

        // Hash the refresh token to find it in database
        String tokenHash = hashToken(refreshTokenValue);

        // Find valid refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findValidTokenByHash(
            tokenHash, LocalDateTime.now()
        );

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = tokenOpt.get();

        // Get user details
        Optional<UserResponse> userOpt = userService.getUserById(refreshToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        UserResponse user = userOpt.get();

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), user.getRoles()
        );

        logger.debug("Access token refreshed successfully for user: {}", user.getEmail());

        return new AuthResponse(newAccessToken, getExpiryInSeconds(accessTokenExpiry), user);
    }

    /**
     * Logout user by revoking refresh token
     */
    public void logout(String refreshTokenValue) {
        logger.debug("User logout - revoking refresh token");

        String tokenHash = hashToken(refreshTokenValue);
        refreshTokenRepository.revokeTokenByHash(tokenHash);

        logger.debug("Refresh token revoked successfully");
    }

    /**
     * Logout user from all sessions (revoke all refresh tokens)
     */
    public void logoutAllSessions(String userId) {
        logger.info("Logging out user from all sessions: {}", userId);

        int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId);

        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, userId);
    }

    /**
     * Store refresh token in database with hash
     */
    private void storeRefreshToken(String userId, String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7); // Default 7 days

        // Parse refresh token expiry
        if (refreshTokenExpiry.endsWith("d")) {
            int days = Integer.parseInt(refreshTokenExpiry.substring(0, refreshTokenExpiry.length() - 1));
            expiresAt = LocalDateTime.now().plusDays(days);
        } else if (refreshTokenExpiry.endsWith("h")) {
            int hours = Integer.parseInt(refreshTokenExpiry.substring(0, refreshTokenExpiry.length() - 1));
            expiresAt = LocalDateTime.now().plusHours(hours);
        }

        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        logger.debug("Refresh token stored for user: {}", userId);
    }

    /**
     * Hash token for secure storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Convert duration string to seconds
     */
    private long getExpiryInSeconds(String duration) {
        if (duration.endsWith("m")) {
            return Integer.parseInt(duration.substring(0, duration.length() - 1)) * 60L;
        } else if (duration.endsWith("h")) {
            return Integer.parseInt(duration.substring(0, duration.length() - 1)) * 3600L;
        } else if (duration.endsWith("d")) {
            return Integer.parseInt(duration.substring(0, duration.length() - 1)) * 86400L;
        }
        return 900; // Default 15 minutes
    }

    /**
     * Cleanup expired tokens (called by scheduled job)
     */
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired tokens");

        int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        logger.info("Cleaned up {} expired tokens", deletedCount);
    }
}

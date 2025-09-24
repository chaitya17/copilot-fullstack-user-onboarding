package com.useronboard.service.repository;

import com.useronboard.service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh token repository for JWT token management
 * Handles token storage, validation, and cleanup
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Find valid (non-revoked, non-expired) token by hash
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Find all tokens for a user
     */
    List<RefreshToken> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find valid tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a user (logout all sessions)
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("userId") String userId);

    /**
     * Revoke specific token
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenHash = :tokenHash")
    int revokeTokenByHash(@Param("tokenHash") String tokenHash);

    /**
     * Clean up expired tokens (for scheduled cleanup job)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Clean up revoked tokens older than specified date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :before")
    int deleteRevokedTokensOlderThan(@Param("before") LocalDateTime before);

    /**
     * Count valid tokens for a user (for rate limiting)
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countValidTokensByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
}

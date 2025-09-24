package com.useronboard.service.repository;

import com.useronboard.service.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User audit log repository for compliance and debugging
 * Tracks all user status changes and administrative actions
 */
@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, String> {

    /**
     * Find all audit logs for a specific user
     */
    List<UserAuditLog> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find audit logs for a user with pagination
     */
    Page<UserAuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    List<UserAuditLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Find audit logs performed by a specific admin
     */
    List<UserAuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);

    /**
     * Find recent audit logs (last N days)
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.createdAt >= :fromDate ORDER BY ual.createdAt DESC")
    List<UserAuditLog> findRecentAuditLogs(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Find audit logs for a date range with pagination
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.createdAt BETWEEN :fromDate AND :toDate ORDER BY ual.createdAt DESC")
    Page<UserAuditLog> findAuditLogsByDateRange(@Param("fromDate") LocalDateTime fromDate,
                                               @Param("toDate") LocalDateTime toDate,
                                               Pageable pageable);

    /**
     * Count audit logs by action type
     */
    long countByAction(String action);

    /**
     * Get audit statistics for reporting
     */
    @Query("SELECT ual.action, COUNT(ual) FROM UserAuditLog ual WHERE ual.createdAt >= :fromDate GROUP BY ual.action")
    List<Object[]> getAuditStatistics(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Find status change logs for a user
     */
    @Query("SELECT ual FROM UserAuditLog ual WHERE ual.userId = :userId AND ual.action IN ('APPROVED', 'REJECTED', 'ACTIVATED', 'DEACTIVATED') ORDER BY ual.createdAt DESC")
    List<UserAuditLog> findStatusChangesForUser(@Param("userId") String userId);
}

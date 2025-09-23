package com.useronboard.service.repository;

import com.useronboard.service.entity.User;
import com.useronboard.service.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * User repository with database-portable queries
 * Uses JPQL instead of native SQL for MSSQL/Oracle compatibility
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find users by status with pagination
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Find all pending users for admin approval
     */
    List<User> findByStatusOrderByCreatedAtAsc(UserStatus status);

    /**
     * Find users created after a specific date
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :fromDate ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedAfter(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Find users by role (contains check for comma-separated roles)
     */
    @Query("SELECT u FROM User u WHERE u.roles LIKE CONCAT('%', :role, '%')")
    List<User> findByRolesContaining(@Param("role") String role);

    /**
     * Count users by status
     */
    long countByStatus(UserStatus status);

    /**
     * Update user status - using JPQL for database portability
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status, u.updatedAt = :updatedAt WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") String userId,
                         @Param("status") UserStatus status,
                         @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Find users by partial name match (first or last name)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find active users with specific role
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.roles LIKE CONCAT('%', :role, '%')")
    List<User> findActiveUsersByRole(@Param("role") String role);

    /**
     * Get user statistics for dashboard
     */
    @Query("SELECT " +
           "SUM(CASE WHEN u.status = 'PENDING' THEN 1 ELSE 0 END) as pending, " +
           "SUM(CASE WHEN u.status = 'ACTIVE' THEN 1 ELSE 0 END) as active, " +
           "SUM(CASE WHEN u.status = 'REJECTED' THEN 1 ELSE 0 END) as rejected " +
           "FROM User u")
    Object[] getUserStatistics();
}

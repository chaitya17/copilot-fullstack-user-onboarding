package com.useronboard.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User audit log entity for tracking user status changes
 * Provides audit trail for compliance and debugging
 */
@Entity
@Table(name = "user_audit_log")
@EntityListeners(AuditingEntityListener.class)
public class UserAuditLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "action", length = 50, nullable = false)
    private String action; // CREATED, APPROVED, REJECTED, ACTIVATED, DEACTIVATED

    @Column(name = "performed_by", length = 36)
    private String performedBy; // Admin user ID who performed the action

    @Size(max = 16)
    @Column(name = "old_status", length = 16)
    private String oldStatus;

    @Size(max = 16)
    @Column(name = "new_status", length = 16)
    private String newStatus;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Reference to User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Reference to Admin User who performed the action
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    private User performedByUser;

    // Constructors
    public UserAuditLog() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public UserAuditLog(String userId, String action, String performedBy, String oldStatus, String newStatus, String reason) {
        this();
        this.userId = userId;
        this.action = action;
        this.performedBy = performedBy;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getPerformedByUser() {
        return performedByUser;
    }

    public void setPerformedByUser(User performedByUser) {
        this.performedByUser = performedByUser;
    }

    // Static factory methods for common audit actions
    public static UserAuditLog userCreated(String userId, String reason) {
        return new UserAuditLog(userId, "CREATED", "system", null, "PENDING", reason);
    }

    public static UserAuditLog userApproved(String userId, String adminId, String reason) {
        return new UserAuditLog(userId, "APPROVED", adminId, "PENDING", "ACTIVE", reason);
    }

    public static UserAuditLog userRejected(String userId, String adminId, String reason) {
        return new UserAuditLog(userId, "REJECTED", adminId, "PENDING", "REJECTED", reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAuditLog that = (UserAuditLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserAuditLog{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", performedBy='" + performedBy + '\'' +
                ", oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

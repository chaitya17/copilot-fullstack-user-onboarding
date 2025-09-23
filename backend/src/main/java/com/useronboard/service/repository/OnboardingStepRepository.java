package com.useronboard.service.repository;

import com.useronboard.service.entity.OnboardingStep;
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
 * Onboarding step repository for tracking user progress
 * Manages user onboarding workflow steps
 */
@Repository
public interface OnboardingStepRepository extends JpaRepository<OnboardingStep, String> {

    /**
     * Find all steps for a user ordered by step order
     */
    List<OnboardingStep> findByUserIdOrderByStepOrderAsc(String userId);

    /**
     * Find specific step by user and step name
     */
    Optional<OnboardingStep> findByUserIdAndStepName(String userId, String stepName);

    /**
     * Find completed steps for a user
     */
    List<OnboardingStep> findByUserIdAndIsCompletedTrueOrderByCompletedAtAsc(String userId);

    /**
     * Find incomplete steps for a user
     */
    List<OnboardingStep> findByUserIdAndIsCompletedFalseOrderByStepOrderAsc(String userId);

    /**
     * Get next incomplete step for a user
     */
    @Query("SELECT os FROM OnboardingStep os WHERE os.userId = :userId AND os.isCompleted = false ORDER BY os.stepOrder ASC")
    Optional<OnboardingStep> findNextIncompleteStep(@Param("userId") String userId);

    /**
     * Count completed steps for a user
     */
    long countByUserIdAndIsCompletedTrue(String userId);

    /**
     * Count total steps for a user
     */
    long countByUserId(String userId);

    /**
     * Mark step as completed
     */
    @Modifying
    @Transactional
    @Query("UPDATE OnboardingStep os SET os.isCompleted = true, os.completedAt = :completedAt, os.updatedAt = :updatedAt WHERE os.id = :stepId")
    int markStepCompleted(@Param("stepId") String stepId,
                         @Param("completedAt") LocalDateTime completedAt,
                         @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Update step data
     */
    @Modifying
    @Transactional
    @Query("UPDATE OnboardingStep os SET os.stepData = :stepData, os.updatedAt = :updatedAt WHERE os.id = :stepId")
    int updateStepData(@Param("stepId") String stepId,
                      @Param("stepData") String stepData,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Get onboarding progress percentage for a user
     */
    @Query("SELECT (COUNT(CASE WHEN os.isCompleted = true THEN 1 END) * 100.0 / COUNT(*)) " +
           "FROM OnboardingStep os WHERE os.userId = :userId")
    Double getOnboardingProgress(@Param("userId") String userId);

    /**
     * Find users with incomplete onboarding (for reminders)
     */
    @Query("SELECT DISTINCT os.userId FROM OnboardingStep os WHERE os.isCompleted = false")
    List<String> findUsersWithIncompleteOnboarding();
}

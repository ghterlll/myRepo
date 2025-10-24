package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.domain.exercise.ExerciseLog;
import com.mobile.aura.domain.health.StepCount;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.mapper.ExerciseLogMapper;
import com.mobile.aura.mapper.StepCountMapper;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.service.ActivityLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of ActivityLevelService.
 * Automatically calculates and updates user activity levels based on actual behavior data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLevelServiceImpl implements ActivityLevelService {

    private final StepCountMapper stepCountMapper;
    private final ExerciseLogMapper exerciseLogMapper;
    private final UserHealthProfileMapper healthProfileMapper;

    private static final int ANALYSIS_DAYS = 30;

    @Override
    @Transactional
    public void recalculateAndUpdate(Long userId) {
        // Calculate date range (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(ANALYSIS_DAYS - 1);

        // Fetch step count data
        List<StepCount> stepCounts = stepCountMapper.selectList(
                new LambdaQueryWrapper<StepCount>()
                        .eq(StepCount::getUserId, userId)
                        .ge(StepCount::getRecordDate, startDate)
                        .le(StepCount::getRecordDate, endDate)
        );

        // Fetch exercise data
        List<ExerciseLog> exercises = exerciseLogMapper.selectList(
                new LambdaQueryWrapper<ExerciseLog>()
                        .eq(ExerciseLog::getUserId, userId)
                        .ge(ExerciseLog::getExerciseDate, startDate)
                        .le(ExerciseLog::getExerciseDate, endDate)
                        .isNull(ExerciseLog::getDeletedAt)
        );

        // Calculate metrics
        double avgDailySteps = calculateAverageDailySteps(stepCounts);
        double exercisesPerWeek = calculateExercisesPerWeek(exercises);

        // Calculate new activity level using domain logic
        int newActivityLevel = UserHealthProfile.calculateActivityLevel(avgDailySteps, exercisesPerWeek);

        // Update health profile if exists
        UserHealthProfile healthProfile = healthProfileMapper.findByUserId(userId);

        if (healthProfile == null) {
            // Create new health profile with calculated activity level
            healthProfile = UserHealthProfile.builder()
                    .userId(userId)
                    .activityLvl(newActivityLevel)
                    .build();
            healthProfileMapper.insert(healthProfile);

            log.info("Created health profile with activity level: userId={}, activityLvl={}, avgSteps={}, exercisesPerWeek={}",
                    userId, newActivityLevel, String.format("%.0f", avgDailySteps), String.format("%.1f", exercisesPerWeek));
        } else {
            // Update existing health profile
            boolean updated = healthProfile.updateActivityLevel(newActivityLevel);

            if (updated) {
                healthProfileMapper.updateById(healthProfile);

                log.info("Updated activity level: userId={}, oldLvl={}, newLvl={}, avgSteps={}, exercisesPerWeek={}",
                        userId, healthProfile.getActivityLvl(), newActivityLevel,
                        String.format("%.0f", avgDailySteps), String.format("%.1f", exercisesPerWeek));
            } else {
                log.debug("Activity level unchanged: userId={}, activityLvl={}", userId, newActivityLevel);
            }
        }
    }

    /**
     * Calculate average daily steps from step count records.
     * Returns 0 if no data available.
     *
     * @param stepCounts list of step count records
     * @return average daily steps
     */
    private double calculateAverageDailySteps(List<StepCount> stepCounts) {
        if (stepCounts.isEmpty()) {
            return 0.0;
        }

        long totalSteps = stepCounts.stream()
                .mapToLong(StepCount::getSteps)
                .sum();

        return (double) totalSteps / stepCounts.size();
    }

    /**
     * Calculate exercises per week from exercise log records.
     * Normalizes to weekly frequency based on 30-day period.
     *
     * @param exercises list of exercise log records
     * @return exercises per week
     */
    private double calculateExercisesPerWeek(List<ExerciseLog> exercises) {
        if (exercises.isEmpty()) {
            return 0.0;
        }

        // Convert 30-day count to weekly average
        // 30 days ≈ 4.29 weeks
        return exercises.size() / 4.29;
    }
}

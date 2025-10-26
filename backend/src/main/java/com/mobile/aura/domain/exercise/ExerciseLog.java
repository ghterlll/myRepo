package com.mobile.aura.domain.exercise;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.exercise.DailyWorkoutResp;
import com.mobile.aura.dto.exercise.ExerciseAddReq;
import com.mobile.aura.dto.exercise.ExerciseRangeResp;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Exercise log domain model.
 * Records exercise activities with duration, distance, and calories burned.
 * Simplified design - no separate item tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("exercise_log")
public class ExerciseLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate exerciseDate;
    private String exerciseName;   // Exercise name (e.g., "Running")
    private Integer minutes;
    private BigDecimal distanceKm; // Distance in kilometers
    private Integer kcal;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_EXERCISE_NAME = "Running";
    private static final int MAX_RANGE_DAYS = 31;

    // MET (Metabolic Equivalent of Task) values for different running paces
    // Based on scientific research from ACSM (American College of Sports Medicine)
    // Reference: Ainsworth BE, et al. 2011 Compendium of Physical Activities
    private static final double MET_SLOW_PACE = 6.0;      // > 10 min/km (< 6 km/h)
    private static final double MET_MODERATE_PACE = 8.3;  // 7-10 min/km (6-8.5 km/h)
    private static final double MET_FAST_PACE = 11.0;     // 5-7 min/km (8.5-12 km/h)
    private static final double MET_VERY_FAST_PACE = 14.5; // < 5 min/km (> 12 km/h)

    /**
     * Create a new exercise log from request.
     *
     * @param userId user ID
     * @param req exercise add request
     * @param userWeightKg user's weight in kg (required if calories not provided)
     * @return new exercise log instance
     * @throws BizException if weight is not set and calories not provided
     */
    public static ExerciseLog createFrom(Long userId, ExerciseAddReq req, Double userWeightKg) {
        LocalDate exerciseDate = parseDate(req.getDate());
        String exerciseName = parseExerciseName(req.getExerciseName());
        int kcal = calculateCalories(req, userWeightKg);

        return ExerciseLog.builder()
                .userId(userId)
                .exerciseDate(exerciseDate)
                .exerciseName(exerciseName)
                .minutes(req.getMinutes())
                .distanceKm(req.getDistanceKm())
                .kcal(kcal)
                .build();
    }

    /**
     * Parse date from string or default to today.
     */
    private static LocalDate parseDate(String date) {
        return StringUtils.hasText(date)
                ? LocalDate.parse(date, DATE_FORMATTER)
                : LocalDate.now();
    }

    /**
     * Parse exercise name or default to "Running".
     */
    private static String parseExerciseName(String exerciseName) {
        return StringUtils.hasText(exerciseName)
                ? exerciseName.trim()
                : DEFAULT_EXERCISE_NAME;
    }

    /**
     * Calculate calories burned using professional MET-based formula.
     * Priority:
     * 1) User provided value
     * 2) MET-based calculation using pace (if both distance and duration available)
     * 3) Time-based estimation using moderate MET
     * <p>
     * Formula: Calories = MET × Weight(kg) × Duration(hours)
     * MET value is determined by running pace (speed)
     *
     * @param req exercise add request
     * @param userWeightKg user's weight in kg (required if calories not provided)
     * @return calculated calories
     * @throws BizException if weight is null and calories not provided by user
     */
    private static int calculateCalories(ExerciseAddReq req, Double userWeightKg) {
        // Priority 1: User provided calories
        if (req.getKcal() != null && req.getKcal() > 0) {
            return req.getKcal();
        }

        // Check if weight is available for calculation
        if (userWeightKg == null || userWeightKg <= 0) {
            throw new BizException(CommonStatusEnum.WEIGHT_NOT_SET);
        }

        // Duration in hours
        double durationHours = req.getMinutes() / 60.0;

        // Priority 2: Calculate based on pace (if both distance and duration available)
        if (req.getDistanceKm() != null && req.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            double distanceKm = req.getDistanceKm().doubleValue();
            double speedKmh = distanceKm / durationHours;
            double met = getMETFromSpeed(speedKmh);

            // Calories = MET × Weight(kg) × Duration(hours)
            return (int) Math.round(met * userWeightKg * durationHours);
        }

        // Priority 3: Time-based estimation using moderate MET (no distance data)
        if (req.getMinutes() > 0) {
            return (int) Math.round(MET_MODERATE_PACE * userWeightKg * durationHours);
        }

        // No sufficient data - should not happen due to validation
        return 0;
    }

    /**
     * Get MET value based on running speed.
     * MET values from ACSM Compendium of Physical Activities.
     *
     * @param speedKmh speed in km/h
     * @return MET value
     */
    private static double getMETFromSpeed(double speedKmh) {
        if (speedKmh < 6.0) {
            return MET_SLOW_PACE;       // Slow jogging
        } else if (speedKmh < 8.5) {
            return MET_MODERATE_PACE;   // Moderate running
        } else if (speedKmh < 12.0) {
            return MET_FAST_PACE;       // Fast running
        } else {
            return MET_VERY_FAST_PACE;  // Very fast running / sprinting
        }
    }

    /**
     * Ensure this log exists and is not deleted.
     *
     * @param log exercise log to check (can be null)
     * @throws com.mobile.aura.support.BizException if log is null or already deleted
     */
    public static void ensureExists(ExerciseLog log) {
        if (log == null || log.getDeletedAt() != null) {
            throw new BizException(CommonStatusEnum.EXERCISE_LOG_NOT_FOUND);
        }
    }

    /**
     * Mark this log as deleted (soft delete).
     */
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Check if this log is accessible by the given user.
     *
     * @param userId user ID to check
     * @throws com.mobile.aura.support.BizException if user doesn't have access
     */
    public void ensureAccessibleBy(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new BizException(CommonStatusEnum.FORBIDDEN);
        }
    }

    /**
     * Validate date range does not exceed maximum allowed days.
     *
     * @param fromDate start date
     * @param toDate end date
     * @throws BizException if range exceeds MAX_RANGE_DAYS
     */
    private static void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate) + 1; // +1 to include both dates
        if (daysBetween > MAX_RANGE_DAYS) {
            throw new BizException(
                    CommonStatusEnum.DATE_RANGE_TOO_LARGE.getCode(),
                    String.format("Maximum %d days allowed, requested %d days",
                            MAX_RANGE_DAYS, daysBetween)
            );
        }
    }

    /**
     * Convert list of exercise logs to range response with daily summaries.
     * Fills missing dates with empty day responses.
     * Calculates aggregate statistics for the entire period.
     *
     * @param logs list of exercise logs (can be empty)
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @return ExerciseRangeResp with daily summaries and statistics
     * @throws BizException if date range exceeds MAX_RANGE_DAYS
     */
    public static ExerciseRangeResp toRangeResponse(List<ExerciseLog> logs, LocalDate fromDate, LocalDate toDate) {
        // Validate date range
        validateDateRange(fromDate, toDate);
        // Build map for fast lookup: date -> list of logs for that date
        Map<LocalDate, List<ExerciseLog>> logsByDate = new HashMap<>();
        for (ExerciseLog log : logs) {
            logsByDate.computeIfAbsent(log.getExerciseDate(), k -> new ArrayList<>()).add(log);
        }

        // Fill missing dates and build daily workout responses
        List<DailyWorkoutResp> dailyWorkouts = new ArrayList<>();
        int totalKcal = 0;
        int totalMinutes = 0;
        BigDecimal totalDistanceKm = BigDecimal.ZERO;
        int activeDays = 0;
        int dayCount = 0;

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            List<ExerciseLog> logsForDay = logsByDate.getOrDefault(date, List.of());
            int dailyKcal = logsForDay.stream().mapToInt(ExerciseLog::getKcal).sum();

            // Create daily workout response
            dailyWorkouts.add(DailyWorkoutResp.of(
                    date.format(DATE_FORMATTER),
                    dailyKcal,
                    logsForDay
            ));

            // Accumulate statistics
            if (!logsForDay.isEmpty()) {
                totalKcal += dailyKcal;
                totalMinutes += logsForDay.stream().mapToInt(ExerciseLog::getMinutes).sum();
                totalDistanceKm = totalDistanceKm.add(
                    logsForDay.stream()
                        .map(ExerciseLog::getDistanceKm)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                );
                activeDays++;
            }
            dayCount++;
        }

        int avgDailyKcal = dayCount > 0 ? totalKcal / dayCount : 0;

        return ExerciseRangeResp.builder()
                .items(dailyWorkouts)
                .totalKcal(totalKcal)
                .totalMinutes(totalMinutes)
                .totalDistanceKm(totalDistanceKm)
                .avgDailyKcal(avgDailyKcal)
                .activeDays(activeDays)
                .build();
    }
}

package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.user.UserProfileUpdateReq;
import com.mobile.aura.dto.water.WaterConfigResp;
import com.mobile.aura.dto.weight.WeightLatestResp;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * User health profile domain model.
 * Contains weight tracking, fitness goals, and health-related preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_health_profile")
public class UserHealthProfile {

    @TableId
    private Long id;

    private Long userId;
    private Integer heightCm;
    private Double initialWeightKg;
    private LocalDate initialWeightAt;
    private Double latestWeightKg;
    private LocalDate latestWeightAt;
    private Double targetWeightKg;
    private LocalDate targetDeadline;
    private Integer dietRule;
    private Integer activityLvl;
    private Integer goalWaterIntakeMl;
    private String quickRecordsMl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Create initial health profile with first weight submission.
     *
     * @param userId user ID
     * @param weightKg weight in kilograms
     * @param recordDate date of weight record
     * @return new health profile instance
     */
    public static UserHealthProfile createInitial(Long userId, Double weightKg, LocalDate recordDate) {
        return UserHealthProfile.builder()
                .userId(userId)
                .initialWeightKg(weightKg)
                .initialWeightAt(recordDate)
                .latestWeightKg(weightKg)
                .latestWeightAt(recordDate)
                .build();
    }

    /**
     * Create empty health profile for new user registration.
     * All fields are null except userId and timestamps.
     *
     * @param userId user ID
     * @return new empty health profile instance
     */
    public static UserHealthProfile createForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return UserHealthProfile.builder()
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Apply health profile updates from request DTO.
     * Handles all health-related fields including weight, height, and goals.
     * Note: activityLvl is NOT updated here - it's automatically calculated based on user behavior.
     *
     * @param request profile update request
     */
    public void applyUpdates(UserProfileUpdateReq request) {
        // Update simple fields
        updateIfPresent(request.getHeightCm(), this::setHeightCm);
        // activityLvl is intentionally NOT updated from request - it's auto-calculated
        updateIfPresent(request.getTargetWeightKg(), this::setTargetWeightKg);
        updateTargetDeadlineIfPresent(request.getTargetDeadline());

        // Update weight fields (used for initialization or direct setting)
        updateIfPresent(request.getInitialWeightKg(), this::setInitialWeightKg);
        updateIfPresent(request.getInitialWeightAt(), this::setInitialWeightAt);
        updateIfPresent(request.getLatestWeightKg(), this::setLatestWeightKg);
        updateIfPresent(request.getLatestWeightAt(), this::setLatestWeightAt);
    }

    /**
     * Update target deadline if present in string format.
     *
     * @param deadlineString deadline in format "yyyy-MM-dd"
     */
    private void updateTargetDeadlineIfPresent(String deadlineString) {
        Optional.ofNullable(deadlineString)
                .filter(StringUtils::hasText)
                .map(s -> LocalDate.parse(s, DATE_FORMATTER))
                .ifPresent(this::setTargetDeadline);
    }

    /**
     * Generic update helper that only applies non-null values.
     *
     * @param value value to update
     * @param setter setter method reference
     * @param <T> type of the value
     */
    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        Optional.ofNullable(value).ifPresent(setter);
    }

    /**
     * Convert to WeightLatestResp DTO.
     * Formats dates and packages weight information for API response.
     *
     * @return weight latest response DTO
     */
    public WeightLatestResp toWeightLatestResp() {
        return new WeightLatestResp(
                formatDate(this.latestWeightAt),
                this.latestWeightKg,
                formatDate(this.initialWeightAt),
                this.initialWeightKg,
                this.targetWeightKg,
                formatDate(this.targetDeadline)
        );
    }

    /**
     * Format LocalDate to string, returns null if date is null.
     *
     * @param date date to format
     * @return formatted date string or null
     */
    private static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /**
     * Update initial weight information.
     *
     * @param weightKg weight in kilograms
     * @param recordedAt date of weight record
     */
    public void updateInitialWeight(Double weightKg, LocalDate recordedAt) {
        this.initialWeightKg = weightKg;
        this.initialWeightAt = recordedAt;
    }

    /**
     * Update latest weight information.
     *
     * @param weightKg weight in kilograms
     * @param recordedAt date of weight record
     */
    public void updateLatestWeight(Double weightKg, LocalDate recordedAt) {
        this.latestWeightKg = weightKg;
        this.latestWeightAt = recordedAt;
    }

    /**
     * Check if need to update initial weight based on new record date.
     *
     * @param recordDate new weight record date
     * @return true if new record is earlier than current initial date
     */
    public boolean shouldUpdateInitial(LocalDate recordDate) {
        return this.initialWeightAt == null || recordDate.isBefore(this.initialWeightAt);
    }

    /**
     * Check if need to update latest weight based on new record date.
     *
     * @param recordDate new weight record date
     * @return true if new record is later than or equal to current latest date
     */
    public boolean shouldUpdateLatest(LocalDate recordDate) {
        return this.latestWeightAt == null ||
               recordDate.isAfter(this.latestWeightAt) ||
               recordDate.isEqual(this.latestWeightAt);
    }

    /**
     * Parse quick records string to list of integers.
     *
     * @return list of quick record amounts in ml
     */
    public List<Integer> parseQuickRecords() {
        if (this.quickRecordsMl == null || this.quickRecordsMl.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(this.quickRecordsMl.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * Add a new amount to quick records if not already present.
     * Keeps only the most recent 4 unique values.
     *
     * @param amountMl amount to add
     */
    public void addToQuickRecords(int amountMl) {
        List<Integer> records = parseQuickRecords();

        // Remove if already exists (to re-add at front)
        records.remove(Integer.valueOf(amountMl));

        // Add to front
        records.addFirst(amountMl);

        // Keep only first 4
        if (records.size() > 4) {
            records = records.subList(0, 4);
        }

        // Convert back to string
        this.quickRecordsMl = records.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

    }

    /**
     * Update quick records list.
     *
     * @param records list of quick record amounts (max 4)
     */
    public void updateQuickRecords(List<Integer> records) {
        if (records == null || records.isEmpty()) {
            this.quickRecordsMl = null;
            return;
        }

        // Keep only first 4
        List<Integer> limited = records.size() > 4
                ? records.subList(0, 4)
                : records;

        this.quickRecordsMl = limited.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Update water intake goal.
     *
     * @param goalMl goal in milliliters
     */
    public void updateGoalWaterIntake(Integer goalMl) {
        this.goalWaterIntakeMl = goalMl;
    }

    /**
     * Get water intake goal, throws exception if not set.
     *
     * @return goal in milliliters
     * @throws BizException if goal is not set
     */
    public int getGoalWaterIntake() {
        if (this.goalWaterIntakeMl == null) {
            throw new BizException(CommonStatusEnum.WATER_GOAL_NOT_SET);
        }
        return this.goalWaterIntakeMl;
    }

    /**
     * Build water config response with current intake.
     *
     * @param currentIntakeMl current day's water intake in ml
     * @return WaterConfigResp
     */
    public WaterConfigResp toWaterConfigResp(int currentIntakeMl) {
        return WaterConfigResp.builder()
                .quickRecordsMl(parseQuickRecords())
                .goalWaterIntakeMl(getGoalWaterIntake())
                .currentIntakeMl(currentIntakeMl)
                .build();
    }

    /**
     * Domain service: Calculate activity level based on actual user behavior.
     * Uses last 30 days of step counts and exercise frequency.
     * <p>
     * Algorithm:
     * - Level 1 (Sedentary): avg steps < 3000 AND exercises < 1/week
     * - Level 2 (Light): avg steps 3000-5000 OR exercises 1-2/week
     * - Level 3 (Moderate): avg steps 5000-8000 OR exercises 2-3/week
     * - Level 4 (Heavy): avg steps 8000-12000 OR exercises 3-5/week
     * - Level 5 (Athletic): avg steps > 12000 OR exercises 5+/week
     *
     * @param avgDailySteps average daily steps over the last 30 days
     * @param exercisesPerWeek number of exercise sessions per week
     * @return activity level (1-5)
     */
    public static int calculateActivityLevel(double avgDailySteps, double exercisesPerWeek) {
        // If both metrics are very low, definitely sedentary
        if (avgDailySteps < 3000 && exercisesPerWeek < 1) {
            return 1; // Sedentary
        }

        // Athletic - either very high steps or very frequent exercise
        if (avgDailySteps > 12000 || exercisesPerWeek >= 5) {
            return 5; // Athletic
        }

        // Heavy activity - high steps or frequent exercise
        if (avgDailySteps > 8000 || exercisesPerWeek >= 3) {
            return 4; // Heavy
        }

        // Moderate activity - moderate steps or some exercise
        if (avgDailySteps > 5000 || exercisesPerWeek >= 2) {
            return 3; // Moderate
        }

        // Light activity - low to moderate steps or occasional exercise
        return 2; // Light
    }

    /**
     * Update activity level if it has changed.
     *
     * @param newLevel new activity level (1-5)
     * @return true if activity level was updated, false if it remained the same
     */
    public boolean updateActivityLevel(int newLevel) {
        if (this.activityLvl == null || !this.activityLvl.equals(newLevel)) {
            this.activityLvl = newLevel;
            return true;
        }
        return false;
    }

    /**
     * Domain service: Estimate daily target calories based on user profile.
     * Uses the Mifflin-St Jeor equation for BMR and activity level multiplier.
     * Falls back to 2000 kcal if insufficient data.
     *
     * @param gender user gender (0=female, 1=male, 2=other)
     * @param age user age
     * @param heightCm user height in centimeters
     * @param weightKg user weight in kilograms
     * @param activityLvl activity level (1-5)
     * @return estimated daily target calories
     */
    public static int estimateDailyTargetCalories(Integer gender, Integer age,
                                                  Integer heightCm, Double weightKg,
                                                  Integer activityLvl) {
        // Validate required fields
        if (age == null || heightCm == null || weightKg == null) {
            return 2000; // Default fallback
        }

        // Calculate BMR using Mifflin-St Jeor equation
        double bmr;
        if (gender != null && gender == 0) { // Female
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        } else { // Male or other
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
        }

        // Activity factor (1-5 scale)
        double activityFactor = switch (activityLvl == null ? 2 : activityLvl) {
            case 1 -> 1.2;    // Sedentary
            case 2 -> 1.375;  // Light activity
            case 3 -> 1.55;   // Moderate activity
            case 4 -> 1.725;  // Heavy activity
            case 5 -> 1.9;    // Athletic/competitive
            default -> 1.375;
        };

        // Calculate TDEE and round to nearest 10
        int tdee = (int) Math.round(bmr * activityFactor);
        return Math.max(1200, (tdee + 5) / 10 * 10);
    }
}

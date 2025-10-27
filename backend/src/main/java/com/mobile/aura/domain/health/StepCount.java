package com.mobile.aura.domain.health;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.step.*;
import com.mobile.aura.dto.step.StepSyncResp.SyncStatus;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step count domain model (Aggregate Root).
 * Represents daily step count records with business logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("step_count")
public class StepCount {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_STEPS = 100_000;
    private static final int MIN_STEPS = 0;
    private static final double STEP_LENGTH_METERS = 0.7; // Average step length
    private static final int STEPS_PER_MINUTE = 100; // Estimated active pace
    private static final double CALORIE_FACTOR = 0.00045; // Calories per step per kg

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private Integer steps;
    private BigDecimal distanceKm;
    private Integer kcal;
    private Integer activeMinutes;
    private String dataSource; // "Sensor", "GoogleFit", "HuaweiHealth", "Manual"

    // Sync control fields
    private Long syncSequence;
    private Integer version;

    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new step count record.
     *
     * @param userId user ID
     * @param recordDate record date
     * @param req sync request containing step data
     * @param userWeightKg user's weight in kg (for calorie calculation)
     * @return new StepCount instance
     */
    public static StepCount create(Long userId, LocalDate recordDate, StepSyncReq req, Double userWeightKg) {
        validateSteps(req.getSteps());

        StepCount stepCount = StepCount.builder()
                .userId(userId)
                .recordDate(recordDate != null ? recordDate : LocalDate.now())
                .steps(req.getSteps())
                .syncSequence(req.getSyncSequence())
                .dataSource(req.getDataSource() != null ? req.getDataSource() : "Sensor")
                .version(1)
                .syncedAt(LocalDateTime.now())
                .build();

        // Calculate derived metrics
        stepCount.calculateMetrics(userWeightKg);

        return stepCount;
    }

    /**
     * Update step count with new data.
     * Only updates if new data is more recent (larger syncSequence).
     *
     * @param req sync request containing new step data
     * @param userWeightKg user's weight in kg
     * @return true if updated, false if rejected (old data)
     */
    public boolean updateSteps(StepSyncReq req, Double userWeightKg) {
        validateSteps(req.getSteps());

        // Reject old data
        if (req.getSyncSequence() <= this.syncSequence) {
            return false;
        }

        // Only update if steps increased (prevent data regression)
        if (req.getSteps() > this.steps) {
            this.steps = req.getSteps();
            this.syncSequence = req.getSyncSequence();
            this.dataSource = req.getDataSource();
            this.version++;
            this.syncedAt = LocalDateTime.now();

            // Recalculate metrics
            calculateMetrics(userWeightKg);

            return true;
        }

        // Steps not increased, but update syncSequence to prevent re-sync
        this.syncSequence = req.getSyncSequence();
        this.syncedAt = LocalDateTime.now();

        return true;
    }

    /**
     * Calculate derived metrics: distance, calories, active minutes.
     *
     * @param userWeightKg user's weight in kg (for calorie calculation)
     */
    public void calculateMetrics(Double userWeightKg) {
        // Calculate distance (km)
        double distanceMeters = this.steps * STEP_LENGTH_METERS;
        this.distanceKm = BigDecimal.valueOf(distanceMeters / 1000.0)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate active minutes
        this.activeMinutes = this.steps / STEPS_PER_MINUTE;

        // Calculate calories (requires weight)
        if (userWeightKg != null && userWeightKg > 0) {
            double calories = this.steps * userWeightKg * CALORIE_FACTOR;
            this.kcal = (int) Math.round(calories);
        } else {
            this.kcal = 0;
        }
    }

    /**
     * Check if this record should accept new sync data.
     *
     * @param newSyncSequence new sync sequence to compare
     * @return true if should accept, false otherwise
     */
    public boolean shouldAcceptSync(Long newSyncSequence) {
        return newSyncSequence > this.syncSequence;
    }

    /**
     * Format date to string.
     *
     * @param date date to format
     * @return formatted date string
     */
    public static String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    /**
     * Validate step count is within acceptable range.
     *
     * @param steps step count to validate
     * @throws BizException if steps is invalid
     */
    private static void validateSteps(Integer steps) {
        if (steps == null || steps < MIN_STEPS || steps > MAX_STEPS) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }
    }

    /**
     * Get formatted date string for this record.
     *
     * @return formatted date string
     */
    public String getFormattedDate() {
        return formatDate(this.recordDate);
    }

    /**
     * Convert this entity to StepDayResp DTO.
     *
     * @return StepDayResp DTO
     */
    public StepDayResp toStepDayResp() {
        return StepDayResp.builder()
                .date(getFormattedDate())
                .steps(this.steps)
                .distanceKm(this.distanceKm.toString())
                .kcal(this.kcal)
                .activeMinutes(this.activeMinutes)
                .dataSource(this.dataSource)
                .version(this.version)
                .syncSequence(this.syncSequence)
                .build();
    }

    /**
     * Convert this entity to StepSyncResp DTO.
     *
     * @param status sync status
     * @param message status message
     * @return StepSyncResp DTO
     */
    public StepSyncResp toSyncResponse(SyncStatus status, String message) {
        return StepSyncResp.builder()
                .date(getFormattedDate())
                .steps(this.steps)
                .version(this.version)
                .syncSequence(this.syncSequence)
                .status(status)
                .message(message)
                .distanceKm(this.distanceKm.toString())
                .kcal(this.kcal)
                .activeMinutes(this.activeMinutes)
                .dataSource(this.dataSource)
                .build();
    }

    /**
     * Create a "zero steps" response for a date with no record.
     *
     * @param date the date
     * @return StepDayResp with zero steps
     */
    public static StepDayResp emptyDayResponse(LocalDate date) {
        return StepDayResp.builder()
                .date(formatDate(date))
                .steps(0)
                .distanceKm("0.00")
                .kcal(0)
                .activeMinutes(0)
                .dataSource("None")
                .version(0)
                .syncSequence(0L)
                .build();
    }

    /**
     * Build batch sync error response.
     *
     * @param date the date
     * @param errorMessage error message
     * @return StepSyncResp with error status
     */
    public static StepSyncResp buildErrorResponse(String date, String errorMessage) {
        return StepSyncResp.builder()
                .date(date)
                .status(SyncStatus.REJECTED)
                .message("Sync failed: " + errorMessage)
                .build();
    }

    /**
     * Build pull steps response.
     *
     * @param updates list of step records
     * @param since original since timestamp
     * @return PullStepsResp
     */
    public static PullStepsResp toPullResponse(List<StepCount> updates, Long since) {
        Long latestTimestamp = updates.isEmpty()
                ? since
                : updates.getFirst().getUpdatedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        List<StepDayResp> respList = updates.stream()
                .map(StepCount::toStepDayResp)
                .toList();

        return PullStepsResp.builder()
                .updates(respList)
                .latestTimestamp(latestTimestamp)
                .hasMore(updates.size() >= 100)
                .build();
    }

    /**
     * Build batch sync response from results.
     *
     * @param results list of individual sync results
     * @return BatchSyncResp
     */
    public static BatchSyncResp toBatchSyncResponse(List<StepSyncResp> results) {
        int successCount = 0;
        int failedCount = 0;

        for (StepSyncResp result : results) {
            if (result.getStatus() == SyncStatus.ACCEPTED) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        return BatchSyncResp.builder()
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    /**
     * Calculate range statistics for a list of step records.
     *
     * @param records list of step count records
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @return StepRangeResp with filled dates and statistics
     */
    public static StepRangeResp toRangeResponse(List<StepCount> records, LocalDate fromDate, LocalDate toDate) {
        // Build map for fast lookup
        Map<LocalDate, StepCount> dataMap = new HashMap<>();
        for (StepCount record : records) {
            dataMap.put(record.getRecordDate(), record);
        }

        // Fill missing dates and calculate totals
        List<StepDayResp> items = new ArrayList<>();
        int totalSteps = 0;
        BigDecimal totalDistanceKm = BigDecimal.ZERO;
        int totalKcal = 0;
        int dayCount = 0;

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            StepCount record = dataMap.get(date);

            if (record != null) {
                items.add(record.toStepDayResp());
                totalSteps += record.getSteps();
                totalDistanceKm = totalDistanceKm.add(record.getDistanceKm());
                totalKcal += record.getKcal();
            } else {
                // Fill missing date with zero
                items.add(emptyDayResponse(date));
            }

            dayCount++;
        }

        int avgDailySteps = dayCount > 0 ? totalSteps / dayCount : 0;

        return StepRangeResp.builder()
                .items(items)
                .totalSteps(totalSteps)
                .totalDistanceKm(totalDistanceKm.setScale(2, RoundingMode.HALF_UP).toString())
                .totalKcal(totalKcal)
                .avgDailySteps(avgDailySteps)
                .build();
    }
}

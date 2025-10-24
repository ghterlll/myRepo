package com.mobile.aura.dto.exercise;

import com.mobile.aura.domain.exercise.ExerciseLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Daily workout summary response DTO.
 * Contains all exercise logs for a specific date with total calories burned.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyWorkoutResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Total calories burned on this day
     */
    private Integer totalKcal;

    /**
     * List of exercise records for this day
     */
    private List<Item> items;

    /**
     * Individual exercise record item.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long id;
        private String exerciseName;
        private Integer minutes;
        private BigDecimal distanceKm;  // Distance in kilometers
        private Integer kcal;
        private String createdAt;       // yyyy-MM-dd HH:mm:ss
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Convert exercise log list to daily workout response.
     *
     * @param date date string in format "yyyy-MM-dd"
     * @param totalKcal total calories burned
     * @param logs list of exercise logs
     * @return daily workout response DTO
     */
    public static DailyWorkoutResp of(String date, int totalKcal, List<ExerciseLog> logs) {
        List<Item> items = logs.stream().map(log -> new Item(
                log.getId(),
                log.getExerciseName(),
                log.getMinutes(),
                log.getDistanceKm(),
                log.getKcal(),
                log.getCreatedAt() == null ? null : TIME_FORMATTER.format(log.getCreatedAt())
        )).collect(Collectors.toList());
        return new DailyWorkoutResp(date, totalKcal, items);
    }
}

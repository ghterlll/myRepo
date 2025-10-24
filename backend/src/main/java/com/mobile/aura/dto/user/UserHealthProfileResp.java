package com.mobile.aura.dto.user;

import com.mobile.aura.domain.user.UserHealthProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Response DTO for user health profile information.
 * Contains weight tracking, fitness goals, and health-related data.
 */
@Data
@Builder
public class UserHealthProfileResp {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Integer heightCm;
    private Double initialWeightKg;
    private String initialWeightAt;
    private Double latestWeightKg;
    private String latestWeightAt;
    private Double targetWeightKg;
    private String targetDeadline;
    private Integer dietRule;
    private Integer activityLvl;
    private Integer goalWaterIntakeMl;

    /**
     * Create response from UserHealthProfile domain model.
     *
     * @param healthProfile user health profile entity (can be null)
     * @return health profile response
     */
    public static UserHealthProfileResp from(UserHealthProfile healthProfile) {
        if (healthProfile == null) {
            return UserHealthProfileResp.builder().build();
        }

        return UserHealthProfileResp.builder()
                .heightCm(healthProfile.getHeightCm())
                .initialWeightKg(healthProfile.getInitialWeightKg())
                .initialWeightAt(formatDate(healthProfile.getInitialWeightAt()))
                .latestWeightKg(healthProfile.getLatestWeightKg())
                .latestWeightAt(formatDate(healthProfile.getLatestWeightAt()))
                .targetWeightKg(healthProfile.getTargetWeightKg())
                .targetDeadline(formatDate(healthProfile.getTargetDeadline()))
                .dietRule(healthProfile.getDietRule())
                .activityLvl(healthProfile.getActivityLvl())
                .goalWaterIntakeMl(healthProfile.getGoalWaterIntakeMl())
                .build();
    }

    private static String formatDate(LocalDate date) {
        return Optional.ofNullable(date)
                .map(d -> d.format(DATE_FORMATTER))
                .orElse(null);
    }
}

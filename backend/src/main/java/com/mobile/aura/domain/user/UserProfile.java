package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobile.aura.constant.GenderEnum;
import com.mobile.aura.dto.user.UserProfileUpdateReq;
import com.mobile.aura.dto.user.UserRecommendationProfileUpdateReq;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * User basic profile domain model.
 * Contains biographical and personal information only.
 * Health data moved to UserHealthProfile.
 * Social stats moved to UserSocialStats.
 * Privacy settings moved to UserPrivacySettings.
 * Includes recommendation system fields (device_preference, recent_geos, activity_level).
 */
@Data
@TableName("user_profile")
public class UserProfile {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @TableId
    private Long userId;

    // Basic profile fields
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private Integer age;
    private String location;
    private String interests;

    // Recommendation system fields
    private String devicePreference;
    private String recentGeos;
    private String activityLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new user profile.
     *
     * @param userId user ID
     * @return new UserProfile instance
     */
    public static UserProfile createForUser(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        // Set default values for recommendation system fields
        profile.setDevicePreference("Android");
        profile.setActivityLevel("low");
        return profile;
    }

    /**
     * Apply profile updates from request DTO.
     * Only updates basic profile fields.
     *
     * @param request profile update request
     */
    public void applyUpdates(UserProfileUpdateReq request) {
        updateGender(request);
        updateBirthdayIfPresent(request.getBirthday());
        updateIfPresent(request.getAge(), this::setAge);
        updateIfPresent(request.getLocation(), this::setLocation);
        updateIfPresent(request.getInterests(), this::setInterests);
    }

    /**
     * Apply recommendation profile updates from request DTO.
     * Only updates recommendation-related fields.
     *
     * @param request recommendation profile update request
     */
    public void applyRecommendationUpdates(UserRecommendationProfileUpdateReq request) {
        updateInterestsJsonIfPresent(request.getInterests());
        updateIfPresent(request.getDevicePreference(), this::setDevicePreference);
        updateRecentGeosJsonIfPresent(request.getRecentGeos());
        updateIfPresent(request.getActivityLevel(), this::setActivityLevel);
    }

    /**
     * Get gender as text representation.
     *
     * @return gender text (female/male/other)
     */
    public String getGenderText() {
        return GenderEnum.toText(this.gender);
    }

    /**
     * Get birthday formatted as string.
     *
     * @return birthday in format "yyyy-MM-dd" or null
     */
    public String getBirthdayFormatted() {
        return Optional.ofNullable(this.birthday)
                .map(date -> date.format(DATE_FORMATTER))
                .orElse(null);
    }

    private void updateGender(UserProfileUpdateReq request) {
        Optional.ofNullable(request.getGenderCode())
                .or(() -> Optional.ofNullable(GenderEnum.toCode(request.getGender())))
                .ifPresent(this::setGender);
    }

    private void updateBirthdayIfPresent(String birthdayString) {
        Optional.ofNullable(birthdayString)
                .filter(StringUtils::hasText)
                .map(s -> LocalDate.parse(s, DATE_FORMATTER))
                .ifPresent(this::setBirthday);
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        Optional.ofNullable(value).ifPresent(setter);
    }

    /**
     * Update interests field with JSON serialization
     */
    private void updateInterestsJsonIfPresent(List<String> interests) {
        if (interests != null) {
            this.interests = toJson(interests);
        }
    }

    /**
     * Update recent geos field with JSON serialization
     */
    private void updateRecentGeosJsonIfPresent(List<UserRecommendationProfileUpdateReq.GeoLocation> geos) {
        if (geos != null) {
            this.recentGeos = toJson(geos);
        }
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
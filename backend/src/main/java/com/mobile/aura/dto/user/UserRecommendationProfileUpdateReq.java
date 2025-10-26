package com.mobile.aura.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating user recommendation-related profile fields.
 * All fields are optional for PATCH semantics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendationProfileUpdateReq {

    /**
     * User interests as a list of tags (e.g., ["fitness", "nutrition", "yoga"])
     * Will be stored as JSON in database
     */
    private List<String> interests;

    /**
     * User's preferred device type
     * Valid values: iOS, Android, Web
     */
    @Pattern(regexp = "iOS|Android|Web", message = "devicePreference must be iOS, Android, or Web")
    private String devicePreference;

    /**
     * Recent geographic locations visited by the user
     * List of location objects with lat, lon, timestamp
     */
    private List<GeoLocation> recentGeos;

    /**
     * User activity level for recommendation algorithm
     * Valid values: low, medium, high
     */
    @Pattern(regexp = "low|medium|high", message = "activityLevel must be low, medium, or high")
    private String activityLevel;

    /**
     * Geographic location data structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        private Double lat;
        private Double lon;
        private String timestamp;  // ISO 8601 format
    }
}

package com.mobile.aura.dto.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobile.aura.domain.user.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Response DTO for user recommendation-related profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendationProfileResp {
    private List<String> interests;
    private String devicePreference;
    private List<UserRecommendationProfileUpdateReq.GeoLocation> recentGeos;
    private String activityLevel;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create response from UserProfile domain model
     */
    public static UserRecommendationProfileResp from(UserProfile profile) {
        if (profile == null) {
            return new UserRecommendationProfileResp();
        }

        UserRecommendationProfileResp resp = new UserRecommendationProfileResp();
        resp.setDevicePreference(profile.getDevicePreference());
        resp.setActivityLevel(profile.getActivityLevel());

        // Parse JSON fields
        resp.setInterests(parseJsonList(profile.getInterests(), new TypeReference<List<String>>() {}));
        resp.setRecentGeos(parseJsonList(profile.getRecentGeos(),
                new TypeReference<List<UserRecommendationProfileUpdateReq.GeoLocation>>() {}));

        return resp;
    }

    /**
     * Helper method to parse JSON string to list
     */
    private static <T> List<T> parseJsonList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}

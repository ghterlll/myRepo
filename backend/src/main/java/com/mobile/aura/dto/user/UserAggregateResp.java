package com.mobile.aura.dto.user;

import com.mobile.aura.domain.user.User;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.domain.user.UserProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

@Data
@Builder
public class UserAggregateResp {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Long userId;
    private String phone;
    private String nickname;
    private Integer status;
    private String city;

    private String avatarUrl;
    private Integer activityLvl;
    private Integer genderCode;
    private String gender;
    private String birthday;
    private Integer heightCm;
    private Double initialWeightKg;
    private LocalDate initialWeightAt;
    private Double latestWeightKg;
    private LocalDate latestWeightAt;
    private Double targetWeightKg;
    private String targetDeadline;
    private Integer age;
    private String location;
    private String deviceType;
    private String interests;

    /**
     * Create response from User, UserProfile and UserHealthProfile domain models.
     * Encapsulates all mapping logic in the DTO layer.
     *
     * @param user user entity
     * @param profile user basic profile (can be null)
     * @param healthProfile user health profile (can be null)
     * @return aggregate response
     */
    public static UserAggregateResp from(User user, UserProfile profile, UserHealthProfile healthProfile) {
        return UserAggregateResp.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .city(user.getCity())
                .avatarUrl(user.getAvatarUrl())
                .genderCode(getFromProfile(profile, UserProfile::getGender))
                .gender(getFromProfile(profile, UserProfile::getGenderText))
                .birthday(getFromProfile(profile, UserProfile::getBirthdayFormatted))
                .age(getFromProfile(profile, UserProfile::getAge))
                .location(getFromProfile(profile, UserProfile::getLocation))
                .interests(getFromProfile(profile, UserProfile::getInterests))
                .heightCm(getFromHealth(healthProfile, UserHealthProfile::getHeightCm))
                .initialWeightKg(getFromHealth(healthProfile, UserHealthProfile::getInitialWeightKg))
                .initialWeightAt(getFromHealth(healthProfile, UserHealthProfile::getInitialWeightAt))
                .latestWeightKg(getFromHealth(healthProfile, UserHealthProfile::getLatestWeightKg))
                .latestWeightAt(getFromHealth(healthProfile, UserHealthProfile::getLatestWeightAt))
                .targetWeightKg(getFromHealth(healthProfile, UserHealthProfile::getTargetWeightKg))
                .targetDeadline(formatDate(getFromHealth(healthProfile, UserHealthProfile::getTargetDeadline)))
                .activityLvl(getFromHealth(healthProfile, UserHealthProfile::getActivityLvl))
                .deviceType(null)
                .build();
    }

    private static <T> T getFromProfile(UserProfile profile, Function<UserProfile, T> extractor) {
        return Optional.ofNullable(profile)
                .map(extractor)
                .orElse(null);
    }

    private static <T> T getFromHealth(UserHealthProfile healthProfile, Function<UserHealthProfile, T> extractor) {
        return Optional.ofNullable(healthProfile)
                .map(extractor)
                .orElse(null);
    }

    private static String formatDate(LocalDate date) {
        return Optional.ofNullable(date)
                .map(d -> d.format(DATE_FORMATTER))
                .orElse(null);
    }
}
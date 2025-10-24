package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.GenderEnum;
import com.mobile.aura.dto.user.UserProfileUpdateReq;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * User basic profile domain model.
 * Contains biographical and personal information only.
 * Health data moved to UserHealthProfile.
 * Social stats moved to UserSocialStats.
 * Privacy settings moved to UserPrivacySettings.
 */
@Data
@TableName("user_profile")
public class UserProfile {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @TableId
    private Long userId;

    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private Integer age;
    private String location;
    private String interests;
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
}
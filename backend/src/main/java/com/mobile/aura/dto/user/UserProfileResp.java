package com.mobile.aura.dto.user;

import com.mobile.aura.domain.user.UserProfile;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for user basic profile information.
 * Contains biographical and personal information only.
 */
@Data
@Builder
public class UserProfileResp {
    private String bio;
    private Integer genderCode;
    private String gender;
    private String birthday;
    private Integer age;
    private String location;
    private String interests;

    /**
     * Create response from UserProfile domain model.
     *
     * @param profile user profile entity (can be null)
     * @return profile response
     */
    public static UserProfileResp from(UserProfile profile) {
        if (profile == null) {
            return UserProfileResp.builder().build();
        }

        return UserProfileResp.builder()
                .bio(profile.getBio())
                .genderCode(profile.getGender())
                .gender(profile.getGenderText())
                .birthday(profile.getBirthdayFormatted())
                .age(profile.getAge())
                .location(profile.getLocation())
                .interests(profile.getInterests())
                .build();
    }
}

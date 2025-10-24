package com.mobile.aura.dto.user;

import com.mobile.aura.domain.user.User;
import lombok.Builder;
import lombok.Data;

/**
 * Basic user information response.
 * Contains only essential public info: user ID, nickname, and avatar.
 * Designed for efficient batch queries and list displays.
 */
@Data
@Builder
public class UserBasicInfoResp {
    private Long userId;
    private String nickname;
    private String avatarUrl;

    /**
     * Create response from User domain model.
     *
     * @param user user entity
     * @return basic info response
     */
    public static UserBasicInfoResp from(User user) {
        return UserBasicInfoResp.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}

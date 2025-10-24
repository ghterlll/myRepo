package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.UserStatus;
import com.mobile.aura.dto.user.UserDtos.RegisterReq;
import com.mobile.aura.dto.user.UserDtos.RegisterWithOtpReq;
import com.mobile.aura.support.BizException;
import lombok.Data;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
@TableName("user")
public class User {
    @TableId
    private Long id;

    private String phone;

    private String email;

    private String password;

    private String nickname;

    private String avatarUrl;

    /** 0 unverified; 1 active; 2 deactivated */
    private Integer status;

    @TableField("region_code")
    private String regionCode;

    private String city;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verify user account status and throw exception if not active
     */
    public void verifyAccountStatus() {
        if (this.status != null && this.status == UserStatus.UNVERIFIED) {
            throw new BizException(CommonStatusEnum.ACCOUNT_UNVERIFIED);
        }
        if (this.status != null && this.status == UserStatus.DEACTIVATED) {
            throw new BizException(CommonStatusEnum.ACCOUNT_DEACTIVATED);
        }
    }

    /**
     * Verify password matches the stored hash and throw exception if not
     */
    public void verifyPasswordOrThrow(String rawPassword, BCryptPasswordEncoder encoder) {
        if (!encoder.matches(rawPassword, this.password)) {
            throw new BizException(CommonStatusEnum.BAD_CREDENTIALS);
        }
    }

    /**
     * Verify password matches the stored hash
     */
    public boolean verifyPassword(String rawPassword, BCryptPasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }

    /**
     * Check if new password is same as current password and throw exception if so
     */
    public void ensurePasswordDifferent(String newPassword, BCryptPasswordEncoder encoder) {
        if (encoder.matches(newPassword, this.password)) {
            throw new BizException(CommonStatusEnum.NEW_PASSWORD_SAME);
        }
    }

    /**
     * Verify email matches user's email
     */
    public void verifyEmail(String email) {
        if (this.email == null || this.email.isBlank()) {
            throw new BizException(CommonStatusEnum.EMAIL_NOT_BOUND);
        }
        if (!this.email.equalsIgnoreCase(email)) {
            throw new BizException(CommonStatusEnum.EMAIL_MISMATCH);
        }
    }

    /**
     * Factory method to create a new ACTIVE user for testing/development
     */
    public static User createNew(RegisterReq req,
                                  java.util.function.Supplier<LocationData> locationFallback,
                                  BCryptPasswordEncoder encoder) {
        return createUserWithStatus(req.getEmail(), req.getPassword(), req.getNickname(), req.getPhone(),
                                   req.getRegionCode(), req.getCity(), locationFallback, encoder, UserStatus.ACTIVE);
    }

    /**
     * Factory method to create a new UNVERIFIED user for production registration
     */
    public static User createNewUnverified(RegisterWithOtpReq req,
                                           java.util.function.Supplier<LocationData> locationFallback,
                                           BCryptPasswordEncoder encoder) {
        return createUserWithStatus(req.getEmail(), req.getPassword(), req.getNickname(), req.getPhone(),
                                   req.getRegionCode(), req.getCity(), locationFallback, encoder, UserStatus.UNVERIFIED);
    }

    /**
     * Private helper to create user with specific status
     */
    private static User createUserWithStatus(String email, String rawPassword, String nickname, String phone,
                                            String regionCode, String city,
                                            java.util.function.Supplier<LocationData> locationFallback,
                                            BCryptPasswordEncoder encoder,
                                            int status) {
        LocationData finalLocation = resolveLocation(regionCode, city, locationFallback);

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(encoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setStatus(status);
        user.setRegionCode(finalLocation.regionCode());
        user.setCity(finalLocation.city());
        return user;
    }

    /**
     * Resolve location: use provided data if available, otherwise use fallback
     */
    private static LocationData resolveLocation(String regionCode, String city,
                                                  java.util.function.Supplier<LocationData> fallback) {
        boolean hasRegion = regionCode != null && !regionCode.isBlank();
        boolean hasCity = city != null && !city.isBlank();

        if (hasRegion || hasCity) {
            return new LocationData(regionCode, city);
        }

        return fallback.get();
    }

    /**
     * Update password (validation already done by Spring Validation on DTO)
     */
    public void updatePassword(String newPassword, BCryptPasswordEncoder encoder) {
        this.password = encoder.encode(newPassword);
    }

    /**
     * Static helper to verify user exists, throws exception if null
     */
    public static User ensureExists(User user) {
        if (user == null) {
            throw new BizException(CommonStatusEnum.USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * Verify email is not already registered
     * Throws exception if email count > 0
     */
    public static void ensureEmailNotRegistered(long emailCount) {
        Optional.of(emailCount > 0)
                .filter(Boolean::booleanValue)
                .ifPresent(unused -> {
                    throw new BizException(CommonStatusEnum.EMAIL_REGISTERED);
                });
    }

    /**
     * Verify old password and ensure new password is different
     */
    public void verifyOldPasswordAndEnsureDifferent(String oldPassword, String newPassword, BCryptPasswordEncoder encoder) {
        if (!verifyPassword(oldPassword, encoder)) {
            throw new BizException(CommonStatusEnum.OLD_PASSWORD_WRONG);
        }
        ensurePasswordDifferent(newPassword, encoder);
    }

    /**
     * Update nickname if provided
     */
    public void updateNicknameIfProvided(String nickname) {
        Optional.ofNullable(nickname)
                .filter(n -> !n.isBlank())
                .map(String::trim)
                .ifPresent(this::setNickname);
    }

    /**
     * Update avatar URL if provided
     */
    public void updateAvatarUrlIfProvided(String avatarUrl) {
        Optional.ofNullable(avatarUrl)
                .filter(url -> !url.isBlank())
                .ifPresent(this::setAvatarUrl);
    }

    /**
     * Activate user account (change status from UNVERIFIED to ACTIVE)
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Value object for location data
     */
    public record LocationData(String regionCode, String city) {}
}

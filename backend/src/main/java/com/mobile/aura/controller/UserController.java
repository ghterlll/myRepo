// file: com/example/demo2/controller/UserController.java
package com.mobile.aura.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.user.User;
import com.mobile.aura.dto.auth.SendResetCodeReq;
import com.mobile.aura.dto.token.RefreshReq;
import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.user.ResetPasswordReq;
import com.mobile.aura.dto.user.UserDtos.*;
import com.mobile.aura.dto.user.UserProfileUpdateReq;
import com.mobile.aura.mapper.UserMapper;
import com.mobile.aura.service.EmailCodeService;
import com.mobile.aura.service.UserProfileService;
import com.mobile.aura.service.UserService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final UserProfileService profileService;
    private final EmailCodeService emailCodeService;
    private final UserMapper userMapper;

    /**
     * Official registration endpoint
     * Creates UNVERIFIED user and sends verification code to email
     * User must verify email before login
     */
    @PostMapping("/register")
    public ResponseResult<?> register(@Valid @RequestBody RegisterWithOtpReq req, HttpServletRequest request) {
        try {
            userService.registerWithOtp(req, request);
            return ResponseResult.success(Map.of(
                    "message", "Registration successful. Please check your email for verification code.",
                    "email", req.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(CommonStatusEnum.EMAIL_REGISTERED.getCode(), e.getMessage());
        }
    }

    /**
     * Send registration verification code to email
     */
    @PostMapping("/register/code")
    public ResponseResult<?> sendRegistrationCode(@Valid @RequestBody SendRegistrationCodeReq req) {
        // Find user by email to get userId
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail()));

        if (user == null) {
            return ResponseResult.fail(CommonStatusEnum.USER_NOT_FOUND.getCode(),
                    "User not found with email: " + req.getEmail());
        }

        emailCodeService.sendRegistrationCode(user.getId(), req.getEmail());
        return ResponseResult.success();
    }

    /**
     * Verify registration code, activate account, and auto-login
     * Changes user status from UNVERIFIED to ACTIVE and returns tokens
     */
    @PostMapping("/register/verify")
    public ResponseResult<?> verifyRegistration(@Valid @RequestBody VerifyRegistrationReq req) {
        TokenPair pair = userService.verifyRegistrationAndActivate(
                req.getEmail(),
                req.getCode(),
                req.getDeviceId()
        );
        return ResponseResult.success(Map.of(
                "accessToken", pair.getAccessToken(),
                "refreshToken", pair.getRefreshToken(),
                "expiresIn", 900  // 15 minutes = 900 seconds
        ));
    }

    @PostMapping("/auth/login")
    public ResponseResult<?> login(@Valid @RequestBody LoginReq req){
        try {
            TokenPair pair = userService.login(req);
            return ResponseResult.success(Map.of(
                    "accessToken", pair.getAccessToken(),
                    "refreshToken", pair.getRefreshToken(),
                    "expiresIn", 900  // 15 minutes = 900 seconds
            ));
        } catch (IllegalArgumentException e){
            return ResponseResult.fail(CommonStatusEnum.BAD_CREDENTIALS.getCode(), e.getMessage());
        }
    }

    // Frontend compatibility alias
    @PostMapping("/login")
    public ResponseResult<?> loginAlias(@Valid @RequestBody LoginReq req){
        return login(req);
    }

    @PostMapping("/auth/refresh")
    public ResponseResult<?> refresh(@RequestBody @Valid RefreshReq req) {
        TokenPair pair = userService.refresh(req.getRefreshToken(), req.getDeviceId());
        return ResponseResult.success(Map.of(
                "accessToken",  pair.getAccessToken(),
                "refreshToken", pair.getRefreshToken(),
                "expiresIn", 900  // 15 minutes = 900 seconds
        ));
    }

    // Frontend compatibility alias
    @PostMapping("/refresh")
    public ResponseResult<?> refreshAlias(@RequestBody @Valid RefreshReq req) {
        return refresh(req);
    }

    @PostMapping("/auth/logout")
    public ResponseResult<?> logout(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
                                    @RequestAttribute(JwtAuthInterceptor.ATTR_DEVICE) String deviceId) {
        userService.logout(userId, deviceId);
        return ResponseResult.success();
    }


    @GetMapping("/me")
    public ResponseResult<?> myProfile(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId){
        return ResponseResult.success(profileService.getAggregate(userId));
    }

    @GetMapping("/me/profile")
    public ResponseResult<?> getMyProfile(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId){
        return ResponseResult.success(profileService.getProfile(userId));
    }

    @GetMapping("/me/health-profile")
    public ResponseResult<?> getMyHealthProfile(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId){
        return ResponseResult.success(profileService.getHealthProfile(userId));
    }

    @PatchMapping("/me/profile")
    public ResponseResult<?> updateMyProfile(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
                                             @RequestBody UserProfileUpdateReq req){
        profileService.updateProfile(userId, req);
        return ResponseResult.success();
    }

    @PostMapping("/me/deactivate")
    public ResponseResult<?> deactivate(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId) {
        userService.deactivate(userId);
        return ResponseResult.success();
    }

    @GetMapping("/me/statistics")
    public ResponseResult<?> myStats(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId) {
        return ResponseResult.success(Map.of(
                "joinedDays",   userService.getJoinedDays(userId),
                "mealCount",    userService.getTotalMealCount(userId),
                "healthyDays",  userService.getHealthyDays(userId)
        ));
    }

    @GetMapping("/me/calories/produced")
    public ResponseResult<?> getCaloriesProduced(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        java.time.LocalDate localDate = date != null
                ? java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : null;
        return ResponseResult.success(Map.of(
                "caloriesProduced", userService.getCaloriesProduced(userId, localDate)
        ));
    }

    @GetMapping("/me/calories/consumed")
    public ResponseResult<?> getCaloriesConsumed(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        java.time.LocalDate localDate = date != null
                ? java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : null;
        return ResponseResult.success(Map.of(
                "caloriesConsumed", userService.getCaloriesConsumed(userId, localDate)
        ));
    }

    @GetMapping("/me/calories/summary")
    public ResponseResult<?> getDailyCaloriesSummary(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        java.time.LocalDate localDate = date != null
                ? java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : null;
        return ResponseResult.success(userService.getDailyCaloriesSummary(userId, localDate));
    }

    @PostMapping("/me/password/reset/code")
    public ResponseResult<?> sendCode(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
                                      @RequestBody SendResetCodeReq req){
        userService.sendResetCode(uid, req.getEmail());
        return ResponseResult.success();
    }

    @PostMapping("/me/password")
    public ResponseResult<?> resetPwd(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
                                      @Valid @RequestBody ResetPasswordReq req){
        userService.resetPassword(uid, req);
        return ResponseResult.success();
    }

    /**
     * Batch get basic user information by user IDs.
     * Returns only essential public info: user ID, nickname, and avatar.
     * Designed for efficient batch queries (e.g., followers/following lists).
     *
     * @param userIds comma-separated user IDs (e.g., "1,2,3")
     * @return list of basic user info
     */
    @GetMapping("/basic-info")
    public ResponseResult<?> getBasicInfo(@RequestParam("ids") List<Long> userIds) {
        return ResponseResult.success(profileService.getBasicInfoBatch(userIds));
    }
}

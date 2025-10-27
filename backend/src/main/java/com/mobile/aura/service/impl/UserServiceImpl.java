package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mobile.aura.constant.DietRule;
import com.mobile.aura.constant.EmailCodePurpose;
import com.mobile.aura.constant.TokenConstants;
import com.mobile.aura.constant.UserStatus;
import com.mobile.aura.domain.auth.RefreshToken;
import com.mobile.aura.domain.auth.RefreshTokenPair;
import com.mobile.aura.domain.exercise.ExerciseLog;
import com.mobile.aura.domain.health.MealLog;
import com.mobile.aura.domain.health.StepCount;
import com.mobile.aura.domain.user.User;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.domain.user.UserProfile;
import com.mobile.aura.domain.user.UserSocialStats;
import com.mobile.aura.dto.user.ResetPasswordReq;
import com.mobile.aura.dto.user.UserDtos.*;
import com.mobile.aura.mapper.ExerciseLogMapper;
import com.mobile.aura.mapper.MealLogMapper;
import com.mobile.aura.mapper.RefreshTokenMapper;
import com.mobile.aura.mapper.StepCountMapper;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.mapper.UserMapper;
import com.mobile.aura.mapper.UserProfileMapper;
import com.mobile.aura.mapper.UserSocialStatsMapper;
import com.mobile.aura.service.EmailCodeService;
import com.mobile.aura.service.GeoLocationService;
import com.mobile.aura.service.UserService;
import com.mobile.aura.support.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper rtMapper;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final EmailCodeService emailCodeService;
    private final UserProfileMapper userProfileMapper;
    private final UserHealthProfileMapper healthProfileMapper;
    private final UserSocialStatsMapper socialStatsMapper;
    private final MealLogMapper mealLogMapper;
    private final ExerciseLogMapper exerciseLogMapper;
    private final StepCountMapper stepCountMapper;
    private final GeoLocationService geoLocationService;

    /**
     * Official registration with email verification
     * Creates UNVERIFIED user and sends verification code to email
     */
    @Override
    @Transactional
    public void registerWithOtp(RegisterWithOtpReq req, HttpServletRequest request) {
        User.ensureEmailNotRegistered(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())));

        User u = User.createNewUnverified(req, () -> getLocationFallback(request), bcrypt);
        userMapper.insert(u);

        createUserProfile(u.getId());

        // Send verification code to email
        emailCodeService.sendRegistrationCode(req.getEmail());
    }

    /**
     * Test/development registration without OTP
     * Creates ACTIVE user immediately
     */
    @Override
    @Transactional
    public void register(RegisterReq req, HttpServletRequest request) {
        User.ensureEmailNotRegistered(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())));

        User u = User.createNew(req, () -> getLocationFallback(request), bcrypt);
        userMapper.insert(u);

        createUserProfile(u.getId());
    }

    /**
     * Get location from request via geolocation service
     */
    private User.LocationData getLocationFallback(HttpServletRequest request) {
        GeoLocationService.LocationInfo location = geoLocationService.getLocationFromRequest(request);
        return new User.LocationData(location.regionCode(), location.city());
    }

    /**
     * Create user profile, health profile, and social stats.
     * Ignores duplicate key conflicts to maintain idempotency.
     */
    private void createUserProfile(Long userId) {
        Optional.of(userId)
                .map(UserProfile::createForUser)
                .ifPresent(profile -> {
                    try {
                        userProfileMapper.insert(profile);
                    } catch (Exception ignore) {
                        // If unique key conflict, maintain idempotency
                    }
                });

        Optional.of(userId)
                .map(UserHealthProfile::createForUser)
                .ifPresent(healthProfile -> {
                    try {
                        healthProfileMapper.insert(healthProfile);
                    } catch (Exception ignore) {
                        // If unique key conflict, maintain idempotency
                    }
                });

        Optional.of(userId)
                .map(UserSocialStats::createForUser)
                .ifPresent(socialStats -> {
                    try {
                        socialStatsMapper.insert(socialStats);
                    } catch (Exception ignore) {
                        // If unique key conflict, maintain idempotency
                    }
                });
    }

    @Override
    public TokenPair login(LoginReq req) {
        // Support both email and phone login
        User u = User.ensureExists(userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail())));
        u.verifyAccountStatus();
        u.verifyPasswordOrThrow(req.getPassword(), bcrypt);

        return generateTokens(u.getId(), req.getDeviceId());
    }

    @Override
    public TokenPair refresh(String refreshToken, String deviceId) {
        String hash = RefreshToken.hashToken(refreshToken);
        RefreshToken token = RefreshToken.ensureValidOrThrow(rtMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hash)));

        String device = RefreshToken.normalizeDeviceId(deviceId);
        String accessToken = JwtUtils.createAccess(token.getUserId(), device, TokenConstants.ACCESS_TTL_SEC);
        String newRefreshToken = token.rotateToken();
        rtMapper.updateById(token);

        return new TokenPair(accessToken, newRefreshToken);
    }

    @Override
    public void logout(Long userId, String deviceId) {
        String device = RefreshToken.normalizeDeviceId(deviceId);

        // Delete the refresh token for this user and device
        rtMapper.delete(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getDeviceId, device));
    }

    @Override
    public void deactivate(Long userId) {
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, UserStatus.DEACTIVATED);
        userMapper.update(null, uw);
    }

    @Override
    @Transactional
    public TokenPair verifyRegistrationAndActivate(String email, String code, String deviceId) {
        emailCodeService.verifyRegistrationCode(email, code);

        User user = User.ensureExists(userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)));

        user.activate();
        userMapper.updateById(user);

        // Auto-login: reuse login logic
        return generateTokens(user.getId(), deviceId);
    }

    /**
     * Generate access and refresh tokens for a user
     */
    private TokenPair generateTokens(Long userId, String deviceId) {
        String device = RefreshToken.normalizeDeviceId(deviceId);
        String accessToken = JwtUtils.createAccess(userId, device, TokenConstants.ACCESS_TTL_SEC);

        RefreshToken existingToken = rtMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getDeviceId, device));

        RefreshTokenPair tokenPair = RefreshToken.createForUserDevice(userId, deviceId, existingToken);
        tokenPair.saveTo(rtMapper);

        return new TokenPair(accessToken, tokenPair.rawToken());
    }

    @Override
    public void sendResetCode(Long userId, String email) {
        User u = User.ensureExists(userMapper.selectById(userId));
        u.verifyEmail(email);
        emailCodeService.sendResetPasswordCode(userId, email);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, ResetPasswordReq req) {
        User u = User.ensureExists(userMapper.selectById(userId));
        u.verifyOldPasswordAndEnsureDifferent(req.getOldPassword(), req.getNewPassword(), bcrypt);
        emailCodeService.verifyOrThrow(userId, req.getEmail(), req.getCode(), EmailCodePurpose.RESET_PASSWORD);
        u.updatePassword(req.getNewPassword(), bcrypt);
        userMapper.updateById(u);
    }

    /**
     * Get total meal count for a user.
     * Service layer: Fetch count via mapper.
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalMealCount(Long userId) {
        Long count = mealLogMapper.selectCount(new LambdaQueryWrapper<MealLog>()
                .eq(MealLog::getUserId, userId)
                .isNull(MealLog::getDeletedAt));
        return count == null ? 0 : count.intValue();
    }

    /**
     * Calculate joined days based on first meal record.
     * Service layer: Fetch first meal via mapper.
     * Domain layer: Calculate days from first meal to today.
     */
    @Override
    @Transactional(readOnly = true)
    public Long getJoinedDays(Long userId) {
        List<MealLog> firstMeal = mealLogMapper.selectList(new LambdaQueryWrapper<MealLog>()
                .eq(MealLog::getUserId, userId)
                .isNull(MealLog::getDeletedAt)
                .orderByAsc(MealLog::getMealDate)
                .last("limit 1"));

        if (firstMeal.isEmpty()) {
            return 0L;
        }

        // Domain logic: Calculate days from first meal to today
        LocalDate firstMealDate = firstMeal.getFirst().getMealDate();
        long days = LocalDate.now().toEpochDay() - firstMealDate.toEpochDay() + 1;
        return Math.max(days, 1);
    }

    /**
     * Count healthy eating days for a user.
     * Service layer: Fetch user profile, health profile, and meal logs via mappers.
     * Domain layer: Calculate target calories and count healthy days.
     */
    @Override
    @Transactional(readOnly = true)
    public Long getHealthyDays(Long userId) {
        // Service layer: Fetch data from mappers
        UserProfile profile = userProfileMapper.selectById(userId);
        UserHealthProfile healthProfile = healthProfileMapper.findByUserId(userId);

        // Domain layer: Calculate target calories using domain service
        int targetCalories = UserHealthProfile.estimateDailyTargetCalories(
                profile != null ? profile.getGender() : null,
                profile != null ? profile.getAge() : null,
                healthProfile != null ? healthProfile.getHeightCm() : null,
                healthProfile != null ? healthProfile.getLatestWeightKg() : null,
                healthProfile != null ? healthProfile.getActivityLvl() : null
        );

        // Service layer: Fetch all meal logs
        List<MealLog> allMealLogs = mealLogMapper.selectList(new LambdaQueryWrapper<MealLog>()
                .eq(MealLog::getUserId, userId)
                .isNull(MealLog::getDeletedAt));

        // Domain layer: Calculate healthy days based on business rules
        return MealLog.countHealthyDays(allMealLogs, targetCalories, DietRule.LOWER, DietRule.UPPER);
    }

    /**
     * Get calories produced (intake) for a specific date.
     * Service layer: Fetch meal logs via mapper.
     * Domain layer: Sum calories using stream operations.
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getCaloriesProduced(Long userId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        // Service layer: Fetch meal logs for the date
        List<MealLog> mealLogs = mealLogMapper.selectList(new LambdaQueryWrapper<MealLog>()
                .eq(MealLog::getUserId, userId)
                .eq(MealLog::getMealDate, effectiveDate)
                .isNull(MealLog::getDeletedAt));

        // Domain logic: Calculate total calories (same as DailySummaryResp)
        return mealLogs.stream()
                .mapToInt(MealLog::getKcal)
                .sum();
    }

    /**
     * Get calories consumed (burned) for a specific date.
     * Service layer: Fetch exercise logs and step count via mappers.
     * Domain layer: Sum calories from both sources.
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getCaloriesConsumed(Long userId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        // Service layer: Fetch exercise logs for the date
        List<ExerciseLog> exerciseLogs = exerciseLogMapper.selectList(new LambdaQueryWrapper<ExerciseLog>()
                .eq(ExerciseLog::getUserId, userId)
                .eq(ExerciseLog::getExerciseDate, effectiveDate)
                .isNull(ExerciseLog::getDeletedAt));

        // Domain logic: Sum exercise calories
        int exerciseCalories = exerciseLogs.stream()
                .mapToInt(ExerciseLog::getKcal)
                .sum();

        // Service layer: Fetch step count for the date
        StepCount stepCount = stepCountMapper.selectOne(new LambdaQueryWrapper<StepCount>()
                .eq(StepCount::getUserId, userId)
                .eq(StepCount::getRecordDate, effectiveDate));

        // Domain logic: Get step calories (0 if no record)
        int stepCalories = stepCount != null && stepCount.getKcal() != null ? stepCount.getKcal() : 0;

        // Total consumed = exercise + steps
        return exerciseCalories + stepCalories;
    }

    /**
     * Get daily calories summary (aggregate).
     * Service layer: Orchestrate calls to getCaloriesProduced and getCaloriesConsumed.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getDailyCaloriesSummary(Long userId, LocalDate date) {
        Integer caloriesProduced = getCaloriesProduced(userId, date);
        Integer caloriesConsumed = getCaloriesConsumed(userId, date);

        return Map.of(
                "caloriesProduced", caloriesProduced,
                "caloriesConsumed", caloriesConsumed
        );
    }
}

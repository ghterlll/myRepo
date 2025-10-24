package com.mobile.aura.service;

import com.mobile.aura.dto.user.ResetPasswordReq;
import com.mobile.aura.dto.user.UserDtos.*;
import com.mobile.aura.support.BizException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service interface for user account and authentication operations.
 * This service provides comprehensive user management functionality including:
 *
 * <h3>Authentication & Authorization</h3>
 * <ul>
 *   <li>User registration with OTP email verification</li>
 *   <li>Login/logout with JWT token-based authentication</li>
 *   <li>Token refresh for maintaining user sessions</li>
 *   <li>Password reset with email verification codes</li>
 * </ul>
 *
 * <h3>Account Management</h3>
 * <ul>
 *   <li>User account activation and deactivation (soft delete)</li>
 *   <li>Multi-device session management</li>
 *   <li>Secure password management with verification</li>
 * </ul>
 *
 * <h3>User Statistics & Analytics</h3>
 * <ul>
 *   <li>Meal tracking statistics (total count, joined days)</li>
 *   <li>Health metrics (healthy eating days based on calorie goals)</li>
 *   <li>Calorie tracking (intake and burn from meals, exercises, steps)</li>
 *   <li>Daily summary aggregation of nutrition and activity data</li>
 * </ul>
 *
 * <h3>Security Considerations</h3>
 * <p>All authentication operations use secure token-based mechanisms.
 * Password reset and registration verification use time-limited OTP codes
 * sent via email. Tokens are device-specific to support multi-device sessions.</p>
 *
 * @see com.mobile.aura.service.impl.UserServiceImpl
 * @author Aura Team
 * @version 2.0
 */
public interface UserService {
    /**
     * Official registration with OTP verification (creates UNVERIFIED user).
     * Creates a new user account in UNVERIFIED status and sends a verification code to the user's email.
     * The user must verify their email using the code before the account becomes ACTIVE.
     * This is the recommended registration flow for production environments.
     *
     * @param req registration request containing email, password, and optional profile data
     * @param request HTTP servlet request for extracting client information (IP, user agent)
     * @throws IllegalArgumentException if email is already registered
     * @throws BizException if email sending fails or validation errors occur
     */
    void registerWithOtp(RegisterWithOtpReq req, HttpServletRequest request);

    /**
     * Test/development registration (creates ACTIVE user immediately).
     * Creates a new user account in ACTIVE status without requiring email verification.
     * This method bypasses the OTP verification step and should only be used in
     * test/development environments. NOT FOR PRODUCTION USE.
     *
     * @param req registration request containing email, password, and optional profile data
     * @param request HTTP servlet request for extracting client information (IP, user agent)
     * @throws IllegalArgumentException if email is already registered
     * @throws BizException if validation errors occur
     */
    void register(RegisterReq req, HttpServletRequest request);

    /**
     * Verify registration code, activate user account, and perform auto-login.
     * Validates the OTP code sent to the user's email during registration.
     * If valid, changes the user status from UNVERIFIED to ACTIVE and generates
     * authentication tokens for automatic login. The verification code expires
     * after a configured time period (typically 10 minutes).
     *
     * @param email user's email address
     * @param code verification code sent to the user's email
     * @param deviceId unique identifier for the user's device
     * @return TokenPair containing access token and refresh token
     * @throws BizException if code is invalid, expired, or user not found
     */
    TokenPair verifyRegistrationAndActivate(String email, String code, String deviceId);

    /**
     * Authenticate user and generate access tokens.
     * Validates user credentials (email and password) and generates a pair of tokens
     * for subsequent authenticated requests. The access token is short-lived (typically 15 minutes)
     * and the refresh token can be used to obtain new access tokens without re-authentication.
     *
     * @param req login request containing email, password, and optional device ID
     * @return TokenPair containing access token (for API requests) and refresh token (for token renewal)
     * @throws IllegalArgumentException if credentials are invalid or user is not active
     * @throws BizException if user account is deactivated or blocked
     */
    TokenPair login(LoginReq req);

    /**
     * Refresh authentication tokens using a valid refresh token.
     * Exchanges a valid refresh token for a new pair of access and refresh tokens.
     * The old refresh token is invalidated after successful refresh. This allows
     * users to maintain authentication without re-entering credentials.
     *
     * @param refreshToken the refresh token obtained from login or previous refresh
     * @param deviceId unique identifier for the user's device (must match the device that obtained the token)
     * @return TokenPair containing new access token and refresh token
     * @throws BizException if refresh token is invalid, expired, or device ID mismatch
     */
    TokenPair refresh(String refreshToken, String deviceId);

    /**
     * Log out user from a specific device.
     * Invalidates all authentication tokens (access and refresh) associated with
     * the user on the specified device. The user will need to log in again to
     * access protected resources from this device. Other devices remain logged in.
     *
     * @param userId the ID of the user to log out
     * @param deviceId the device ID to log out from (other devices unaffected)
     */
    void logout(Long userId, String deviceId);

    /**
     * Deactivate (soft delete) a user account.
     * Marks the user account as deactivated by setting the deleted_at timestamp.
     * The account data is preserved but the user cannot log in or access services.
     * This is a soft delete operation - account data remains in the database for
     * potential recovery or audit purposes.
     *
     * @param userId the ID of the user to deactivate
     * @throws BizException if user not found or already deactivated
     */
    void deactivate(Long userId);

    /**
     * Reset user password with verification code.
     * Changes the user's password after validating a reset code sent to their email.
     * The reset code must be obtained by calling {@link #sendResetCode(Long, String)} first.
     * The code expires after a configured time period (typically 10 minutes).
     * This operation invalidates all existing authentication tokens for security.
     *
     * @param userId the ID of the user resetting password
     * @param req password reset request containing email, verification code, and new password
     * @throws BizException if code is invalid, expired, or email mismatch
     */
    void resetPassword(Long userId, ResetPasswordReq req);

    /**
     * Send password reset verification code to user's email.
     * Generates a one-time verification code and sends it to the user's email address.
     * The code is valid for a limited time (typically 10 minutes) and can only be used once.
     * This method validates that the email matches the user's registered email address.
     *
     * @param userId the ID of the user requesting password reset
     * @param email the user's registered email address (must match user profile)
     * @throws BizException if email doesn't match user profile or email sending fails
     */
    void sendResetCode(Long userId, String email);

    /**
     * Get total meal count for a user.
     * Returns the total number of meal records (non-deleted) created by the user.
     * This count represents all meal logs recorded by the user since registration.
     *
     * @param userId user ID to count meals for
     * @return total count of meal records, returns 0 if no meals recorded
     */
    Integer getTotalMealCount(Long userId);

    /**
     * Calculate the number of days since user joined (based on first meal record).
     * Days are calculated from the date of the first meal record to today (inclusive).
     * For example, if the first meal was recorded today, this returns 1.
     *
     * @param userId user ID to calculate joined days for
     * @return number of days since first meal record, returns 0 if no meals recorded
     */
    Long getJoinedDays(Long userId);

    /**
     * Count the number of healthy eating days for a user.
     * A day is considered "healthy" if total daily calories fall within acceptable range
     * based on the user's profile (gender, age, height, weight, activity level).
     * The acceptable range is determined by diet rules: [targetCalories * LOWER, targetCalories * UPPER].
     *
     * @param userId user ID to count healthy days for
     * @return number of days with healthy eating patterns, returns 0 if no meals or profile data
     */
    Long getHealthyDays(Long userId);

    /**
     * Get total calories produced (intake) for a user on a specific date.
     * Calories produced represents the total caloric intake from all meals consumed.
     * Calculation matches the totalCalories in DailySummaryResp from MealService.
     *
     * @param userId user ID to calculate calories for
     * @param date date to calculate (null defaults to today)
     * @return total calories from all meals on the specified date, returns 0 if no meals
     */
    Integer getCaloriesProduced(Long userId, LocalDate date);

    /**
     * Get total calories consumed (burned) for a user on a specific date.
     * Calories consumed represents the total calories burned through physical activity,
     * calculated from two sources:
     * 1. Exercise logs (running, etc.) - calculated using MET-based formulas
     * 2. Step count - estimated calories from daily steps
     *
     * @param userId user ID to calculate calories for
     * @param date date to calculate (null defaults to today)
     * @return total calories burned from exercise and steps, returns 0 if no activity data
     */
    Integer getCaloriesConsumed(Long userId, LocalDate date);

    /**
     * Get daily calories summary (aggregate) for a user on a specific date.
     * Returns both calories produced (intake) and calories consumed (burned) in a single response.
     * This is a convenience method that aggregates data from meals, exercises, and step counts.
     *
     * @param userId user ID to get summary for
     * @param date date to get summary for (null defaults to today)
     * @return map containing "caloriesProduced" and "caloriesConsumed" keys with integer values
     */
    Map<String, Integer> getDailyCaloriesSummary(Long userId, LocalDate date);
}

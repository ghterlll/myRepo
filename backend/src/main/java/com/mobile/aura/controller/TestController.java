package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.user.UserDtos.RegisterReq;
import com.mobile.aura.service.UserService;
import com.mobile.aura.support.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test/development endpoints
 * NOT FOR PRODUCTION USE
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final UserService userService;

    /**
     * Test registration endpoint - creates ACTIVE users without OTP verification
     * For testing/development purposes only
     */
    @PostMapping("/users/register")
    public ResponseResult<?> registerTest(@Valid @RequestBody RegisterReq req, HttpServletRequest request) {
        userService.register(req, request);
        return ResponseResult.success();
    }

    /**
     * Generate a permanent access token for testing
     * Returns a token that never expires (100 years TTL)
     *
     * Default test user:
     * - User ID: 1000
     * - Email: test@aura.dev
     * - Password: Test123456
     *
     * @param userId User ID (default: 1000)
     * @return Permanent access token
     */
    @GetMapping("/token/permanent")
    public ResponseResult<?> getPermanentToken(@RequestParam(defaultValue = "1000") Long userId) {
        // Generate token with 100 years expiration (effectively permanent)
        long ttlSeconds = 100L * 365 * 24 * 60 * 60; // 100 years in seconds
        String token = JwtUtils.createAccess(userId, "test-device", ttlSeconds);

        return ResponseResult.success(Map.of(
            "accessToken", token,
            "userId", userId,
            "expiresIn", ttlSeconds,
            "expiresInYears", 100,
            "note", "This token is valid for 100 years. Use for testing only!"
        ));
    }
}

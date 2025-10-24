package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.water.*;
import com.mobile.aura.service.WaterService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Water intake tracking controller.
 * Handles water intake submission, querying, and deletion.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/water")
public class WaterController {

    private final WaterService waterService;

    /**
     * Submit/overwrite water intake.
     * Sets the total water intake for the specified date (replaces existing value).
     * Date defaults to today if not provided.
     *
     * @param userId user ID from JWT token
     * @param req submission request
     * @return success response
     */
    @PostMapping
    public ResponseResult<?> submit(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody WaterSubmitReq req) {
        waterService.submit(
                userId,
                req.getDateAsLocalDate(),
                req.getAmountMl()
        );
        return ResponseResult.success();
    }

    /**
     * Increment water intake.
     * Adds the specified amount to existing water intake for the specified date.
     * Date defaults to today if not provided.
     *
     * @param userId user ID from JWT token
     * @param req increment request
     * @return success response
     */
    @PostMapping("/increment")
    public ResponseResult<?> increment(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody WaterIncrementReq req) {
        waterService.increment(
                userId,
                req.getDateAsLocalDate(),
                req.getAmountMl()
        );
        return ResponseResult.success();
    }

    /**
     * Delete water intake record for a specific day.
     *
     * @param userId user ID from JWT token
     * @param req deletion request
     * @return success response
     */
    @DeleteMapping("/day")
    public ResponseResult<?> deleteDay(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody WaterDeleteReq req) {
        waterService.deleteDay(
                userId,
                req.getDateAsLocalDate()
        );
        return ResponseResult.success();
    }

    /**
     * Get water intake for a specific day.
     *
     * @param userId user ID from JWT token
     * @param date date in format "yyyy-MM-dd" (optional, defaults to today)
     * @return daily water intake response
     */
    @GetMapping("/day")
    public ResponseResult<WaterDayResp> day(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        return ResponseResult.success(
                waterService.day(userId, SingleDateReq.parseDateOrToday(date))
        );
    }

    /**
     * Get water intake for a date range.
     * Missing dates are automatically filled with 0 ml.
     *
     * @param userId user ID from JWT token
     * @param req range request with from/to dates
     * @return range water intake response
     */
    @GetMapping("/range")
    public ResponseResult<WaterRangeResp> range(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid WaterRangeReq req) {
        return ResponseResult.success(
                waterService.range(userId, req.getFrom(), req.getTo())
        );
    }

    /**
     * Get water configuration including quick records, goal, and current intake.
     *
     * @param userId user ID from JWT token
     * @param date date for current intake (optional, defaults to today)
     * @return water configuration response
     */
    @GetMapping("/config")
    public ResponseResult<WaterConfigResp> getConfig(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        return ResponseResult.success(
                waterService.getConfig(userId, SingleDateReq.parseDateOrToday(date))
        );
    }

    /**
     * Update water configuration (quick records and/or goal).
     *
     * @param userId user ID from JWT token
     * @param req update request
     * @return success response
     */
    @PutMapping("/config")
    public ResponseResult<?> updateConfig(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody WaterConfigUpdateReq req) {
        waterService.updateConfig(userId, req);
        return ResponseResult.success();
    }
}

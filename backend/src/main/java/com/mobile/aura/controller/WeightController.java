package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.weight.UpdateInitialWeightReq;
import com.mobile.aura.dto.weight.WeightLatestResp;
import com.mobile.aura.dto.weight.WeightRangeReq;
import com.mobile.aura.dto.weight.WeightRangeResp;
import com.mobile.aura.dto.weight.WeightSubmitReq;
import com.mobile.aura.service.WeightService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Weight tracking controller.
 * Handles weight record submission, querying, and retrieval.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weight")
public class WeightController {

    private final WeightService weightService;

    /**
     * Submit/overwrite daily weight record.
     * Creates a new record if none exists for the date, updates if exists.
     *
     * @param userId user ID from JWT token
     * @param req weight submission request
     * @return success response
     */
    @PostMapping
    public ResponseResult<?> submit(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody WeightSubmitReq req) {
        weightService.submit(userId, req);
        return ResponseResult.success();
    }

    /**
     * Get weight records within a date range.
     *
     * @param userId user ID from JWT token
     * @param req range request with from/to dates
     * @return weight range response
     */
    @GetMapping("/range")
    public ResponseResult<WeightRangeResp> range(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid WeightRangeReq req) {
        return ResponseResult.success(
                weightService.range(userId, req.getFrom(), req.getTo())
        );
    }

    /**
     * Get latest, initial, and target weight information.
     *
     * @param userId user ID from JWT token
     * @return weight summary response
     */
    @GetMapping("/latest")
    public ResponseResult<WeightLatestResp> latest(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId) {
        return ResponseResult.success(weightService.latest(userId));
    }

    /**
     * Update initial weight information.
     * Allows users to correct their initial weight if they made a mistake.
     *
     * @param userId user ID from JWT token
     * @param req update initial weight request
     * @return success response
     */
    @PatchMapping("/initial")
    public ResponseResult<?> updateInitialWeight(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody UpdateInitialWeightReq req) {
        weightService.updateInitialWeight(userId, req);
        return ResponseResult.success();
    }
}

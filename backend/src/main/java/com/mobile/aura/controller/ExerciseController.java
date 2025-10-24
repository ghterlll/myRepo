package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.exercise.ExerciseAddReq;
import com.mobile.aura.dto.exercise.ExerciseRangeReq;
import com.mobile.aura.dto.exercise.ExerciseRangeResp;
import com.mobile.aura.service.ExerciseService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Exercise tracking controller.
 * Simplified design for running activity tracking.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exercise")
public class ExerciseController {
    private final ExerciseService exerciseService;

    /**
     * Add exercise log.
     *
     * @param userId user ID from JWT token
     * @param req exercise add request
     * @return created exercise log ID
     */
    @PostMapping
    public ResponseResult<?> add(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody ExerciseAddReq req) {
        return ResponseResult.success(Map.of("id", exerciseService.add(userId, req)));
    }

    /**
     * Delete exercise log.
     *
     * @param userId user ID from JWT token
     * @param id exercise log ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseResult<?> delete(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @PathVariable Long id) {
        exerciseService.delete(userId, id);
        return ResponseResult.success();
    }

    /**
     * Get exercise logs for a specific day.
     *
     * @param userId user ID from JWT token
     * @param date date in format "yyyy-MM-dd" (optional, defaults to today)
     * @return daily workout response with exercise logs
     */
    @GetMapping("/day")
    public ResponseResult<?> day(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = SingleDateReq.parseDateOrToday(date);
        return ResponseResult.success(exerciseService.day(userId, targetDate));
    }

    /**
     * Get exercise logs for a date range.
     * Returns daily workout summaries with aggregate statistics.
     * Maximum range is 31 days.
     * <p>
     * GET /api/v1/exercise/range?from=2025-10-01&to=2025-10-31
     *
     * @param userId user ID from JWT token
     * @param req range request with from/to dates
     * @return range response with daily summaries and statistics
     */
    @GetMapping("/range")
    public ResponseResult<ExerciseRangeResp> getRange(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid ExerciseRangeReq req) {
        ExerciseRangeResp resp = exerciseService.getRange(userId, req.getFrom(), req.getTo());
        return ResponseResult.success(resp);
    }
}

package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.step.*;
import com.mobile.aura.service.StepService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Step count tracking controller.
 * Handles step count synchronization and querying with idempotency guarantees.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/steps")
public class StepController {

    private final StepService stepService;

    /**
     * Sync step count (idempotent operation).
     * Accepts new data only if syncSequence is newer than existing.
     * Updates step count only if new steps are greater.
     * <p>
     * POST /api/v1/steps/sync
     *
     * @param userId user ID from JWT token
     * @param req sync request containing steps, date, and syncSequence
     * @return sync response with status (ACCEPTED/REJECTED/CONFLICT)
     */
    @PostMapping("/sync")
    public ResponseResult<StepSyncResp> syncSteps(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody StepSyncReq req) {
        StepSyncResp resp = stepService.syncSteps(userId, req);
        return ResponseResult.success(resp);
    }

    /**
     * Batch sync step counts.
     * Used for syncing historical data or compensating failed syncs.
     * <p>
     * POST /api/v1/steps/sync/batch
     *
     * @param userId user ID from JWT token
     * @param req batch sync request containing multiple items (max 30)
     * @return batch sync response with success/failure counts
     */
    @PostMapping("/sync/batch")
    public ResponseResult<BatchSyncResp> batchSyncSteps(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody BatchSyncReq req) {
        BatchSyncResp resp = stepService.batchSync(userId, req);
        return ResponseResult.success(resp);
    }

    /**
     * Pull step count updates from server (bidirectional sync).
     * Returns records that are newer than the specified timestamp.
     * Used by clients to pull server changes after local modifications.
     * <p>
     * GET /api/v1/steps/pull?since=<timestamp>
     *
     * @param userId user ID from JWT token
     * @param since timestamp in milliseconds (optional, defaults to 30 days ago)
     * @return pull response containing updated records
     */
    @GetMapping("/pull")
    public ResponseResult<PullStepsResp> pullSteps(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) Long since) {
        PullStepsResp resp = stepService.pullSteps(userId, since);
        return ResponseResult.success(resp);
    }

    /**
     * Get step count for a specific day.
     * Date defaults to today if not provided, but clients SHOULD always pass explicit date
     * to avoid timezone issues.
     * <p>
     * GET /api/v1/steps/day?date=2025-10-17
     * GET /api/v1/steps/day  (defaults to server's today - NOT RECOMMENDED)
     *
     * @param userId user ID from JWT token
     * @param date date string in format "yyyy-MM-dd" (optional but recommended)
     * @return step count for the specified day (0 if no record exists)
     */
    @GetMapping("/day")
    public ResponseResult<StepDayResp> getDay(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        StepDayResp resp = stepService.getDay(userId, date);
        return ResponseResult.success(resp);
    }

    /**
     * Get step counts for a date range.
     * Missing dates are automatically filled with 0 steps.
     * <p>
     * GET /api/v1/steps/range?from=2025-10-10&to=2025-10-17
     *
     * @param userId user ID from JWT token
     * @param req range request with from/to dates
     * @return range response with daily step counts and statistics
     */
    @GetMapping("/range")
    public ResponseResult<StepRangeResp> getRange(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid StepRangeReq req) {
        StepRangeResp resp = stepService.getRange(userId, req.getFrom(), req.getTo());
        return ResponseResult.success(resp);
    }
}

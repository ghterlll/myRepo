package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.domain.health.StepCount;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.step.*;
import com.mobile.aura.dto.step.StepSyncResp.SyncStatus;
import com.mobile.aura.mapper.StepCountMapper;
import com.mobile.aura.service.ActivityLevelService;
import com.mobile.aura.service.StepService;
import com.mobile.aura.service.WeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of StepService.
 * Handles step count synchronization with idempotency guarantees.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StepServiceImpl implements StepService {

    private final StepCountMapper stepCountMapper;
    private final WeightService weightService;
    private final ActivityLevelService activityLevelService;

    @Override
    @Transactional
    public StepSyncResp syncSteps(Long userId, StepSyncReq req) {
        LocalDate recordDate = SingleDateReq.parseDateOrToday(req.getDate());

        // Query existing record
        StepCount existing = stepCountMapper.selectOne(
                new LambdaQueryWrapper<StepCount>()
                        .eq(StepCount::getUserId, userId)
                        .eq(StepCount::getRecordDate, recordDate)
        );

        // If no existing record, create new one
        if (existing == null) {
            return insertNewRecord(userId, recordDate, req);
        }

        // Check if data is newer using syncSequence
        if (!existing.shouldAcceptSync(req.getSyncSequence())) {
            // Reject old data
            return existing.toSyncResponse(SyncStatus.REJECTED, "Data is outdated, server has newer data");
        }

        // Check for version conflict (optimistic locking)
        if (req.getKnownVersion() != null &&
                !req.getKnownVersion().equals(existing.getVersion())) {
            // Version conflict detected
            return existing.toSyncResponse(SyncStatus.CONFLICT, "Version conflict detected, please pull latest data");
        }

        // Update existing record
        return updateExistingRecord(existing, req);
    }

    @Override
    @Transactional
    public BatchSyncResp batchSync(Long userId, BatchSyncReq req) {
        List<StepSyncResp> results = new ArrayList<>();

        for (StepSyncReq item : req.getItems()) {
            try {
                StepSyncResp resp = syncSteps(userId, item);
                results.add(resp);
            } catch (Exception e) {
                log.error("Batch sync failed for userId={}, date={}", userId, item.getDate(), e);
                results.add(StepCount.buildErrorResponse(item.getDate(), e.getMessage()));
            }
        }

        return StepCount.toBatchSyncResponse(results);
    }

    @Override
    @Transactional(readOnly = true)
    public PullStepsResp pullSteps(Long userId, Long since) {
        // Default to 30 days ago if since not provided
        LocalDateTime sinceTime = since != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(since), ZoneId.systemDefault())
                : LocalDateTime.now().minusDays(30);

        // Query records updated after sinceTime
        List<StepCount> updates = stepCountMapper.selectList(
                new LambdaQueryWrapper<StepCount>()
                        .eq(StepCount::getUserId, userId)
                        .gt(StepCount::getUpdatedAt, sinceTime)
                        .orderByDesc(StepCount::getUpdatedAt)
                        .last("LIMIT 100") // Limit to 100 records per pull
        );

        return StepCount.toPullResponse(updates, since);
    }

    @Override
    @Transactional(readOnly = true)
    public StepDayResp getDay(Long userId, String date) {
        LocalDate recordDate = SingleDateReq.parseDateOrToday(date);

        StepCount record = stepCountMapper.selectOne(
                new LambdaQueryWrapper<StepCount>()
                        .eq(StepCount::getUserId, userId)
                        .eq(StepCount::getRecordDate, recordDate)
        );

        return record != null
                ? record.toStepDayResp()
                : StepCount.emptyDayResponse(recordDate);
    }

    @Override
    @Transactional(readOnly = true)
    public StepRangeResp getRange(Long userId, String from, String to) {
        // Parse and normalize date range
        LocalDate fromDate = from != null && !from.isBlank()
                ? LocalDate.parse(from)
                : LocalDate.now().minusDays(6);

        LocalDate toDate = to != null && !to.isBlank()
                ? LocalDate.parse(to)
                : LocalDate.now();

        // Swap if from > to
        if (toDate.isBefore(fromDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        // Query records in range
        List<StepCount> records = stepCountMapper.selectList(
                new LambdaQueryWrapper<StepCount>()
                        .eq(StepCount::getUserId, userId)
                        .ge(StepCount::getRecordDate, fromDate)
                        .le(StepCount::getRecordDate, toDate)
        );

        // Delegate to domain model for response building
        return StepCount.toRangeResponse(records, fromDate, toDate);
    }

    /**
     * Insert new step count record.
     */
    private StepSyncResp insertNewRecord(Long userId, LocalDate recordDate, StepSyncReq req) {
        Double userWeightKg = getUserWeight(userId);
        StepCount stepCount = StepCount.create(userId, recordDate, req, userWeightKg);
        stepCountMapper.insert(stepCount);

        log.info("Created new step record: userId={}, date={}, steps={}, syncSeq={}",
                userId, recordDate, req.getSteps(), req.getSyncSequence());

        // Trigger activity level recalculation
        triggerActivityLevelUpdate(userId);

        return stepCount.toSyncResponse(SyncStatus.ACCEPTED, "Sync successful");
    }

    /**
     * Update existing step count record.
     */
    private StepSyncResp updateExistingRecord(StepCount existing, StepSyncReq req) {
        int oldSteps = existing.getSteps();
        Long oldSyncSeq = existing.getSyncSequence();

        Double userWeightKg = getUserWeight(existing.getUserId());
        boolean updated = existing.updateSteps(req, userWeightKg);

        if (updated) {
            stepCountMapper.updateById(existing);

            log.info("Updated step record: userId={}, date={}, steps: {}→{}, syncSeq: {}→{}",
                    existing.getUserId(), existing.getRecordDate(),
                    oldSteps, existing.getSteps(), oldSyncSeq, existing.getSyncSequence());

            // Trigger activity level recalculation
            triggerActivityLevelUpdate(existing.getUserId());

            return existing.toSyncResponse(SyncStatus.ACCEPTED, "Sync successful");
        } else {
            log.warn("Step update rejected: userId={}, date={}, oldSteps={}, newSteps={}",
                    existing.getUserId(), existing.getRecordDate(), oldSteps, req.getSteps());

            return existing.toSyncResponse(SyncStatus.REJECTED, "Steps not increased or data is outdated");
        }
    }

    /**
     * Get user's latest weight for calorie calculation.
     */
    private Double getUserWeight(Long userId) {
        try {
            var weightResp = weightService.latest(userId);
            if (weightResp != null && weightResp.getLatestWeightKg() != null) {
                return weightResp.getLatestWeightKg();
            }
        } catch (Exception e) {
            log.warn("Failed to get user weight: userId={}", userId, e);
        }
        return null;
    }

    /**
     * Trigger activity level recalculation asynchronously.
     * Failures are logged but don't affect the main operation.
     */
    private void triggerActivityLevelUpdate(Long userId) {
        try {
            activityLevelService.recalculateAndUpdate(userId);
        } catch (Exception e) {
            log.error("Failed to update activity level: userId={}", userId, e);
        }
    }

}

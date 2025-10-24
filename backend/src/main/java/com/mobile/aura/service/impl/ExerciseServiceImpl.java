package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.domain.exercise.ExerciseLog;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.dto.exercise.ExerciseAddReq;
import com.mobile.aura.dto.exercise.DailyWorkoutResp;
import com.mobile.aura.dto.exercise.ExerciseRangeResp;
import com.mobile.aura.mapper.ExerciseLogMapper;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.service.ActivityLevelService;
import com.mobile.aura.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exercise service implementation.
 * Simplified design for running activity tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseLogMapper logMapper;
    private final UserHealthProfileMapper healthProfileMapper;
    private final ActivityLevelService activityLevelService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional
    public Long add(Long userId, ExerciseAddReq req) {
        // Get user's latest weight from health profile
        Double userWeightKg = getUserWeight(userId);

        // Create exercise log with accurate calorie calculation
        ExerciseLog log = ExerciseLog.createFrom(userId, req, userWeightKg);
        logMapper.insert(log);

        // Trigger activity level recalculation
        triggerActivityLevelUpdate(userId);

        return log.getId();
    }

    /**
     * Get user's latest weight from health profile.
     * Returns null if profile doesn't exist or weight is not set.
     *
     * @param userId user ID
     * @return user's weight in kg, or null if not available
     */
    private Double getUserWeight(Long userId) {
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );
        return (profile != null) ? profile.getLatestWeightKg() : null;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long exerciseLogId) {
        ExerciseLog log = logMapper.selectById(exerciseLogId);
        ExerciseLog.ensureExists(log);
        log.ensureAccessibleBy(userId);
        log.markAsDeleted();
        logMapper.updateById(log);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyWorkoutResp day(Long userId, LocalDate date) {
        LocalDate targetDate = (date == null) ? LocalDate.now() : date;

        var logs = logMapper.selectList(new LambdaQueryWrapper<ExerciseLog>()
                .eq(ExerciseLog::getUserId, userId)
                .eq(ExerciseLog::getExerciseDate, targetDate)
                .isNull(ExerciseLog::getDeletedAt)
                .orderByAsc(ExerciseLog::getCreatedAt)
                .orderByAsc(ExerciseLog::getId));

        int totalKcal = logs.stream().mapToInt(ExerciseLog::getKcal).sum();
        return DailyWorkoutResp.of(targetDate.format(DATE_FORMATTER), totalKcal, logs);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseRangeResp getRange(Long userId, String from, String to) {
        // Parse and normalize date range
        LocalDate fromDate = (from != null && !from.isBlank())
                ? LocalDate.parse(from)
                : LocalDate.now().minusDays(6);

        LocalDate toDate = (to != null && !to.isBlank())
                ? LocalDate.parse(to)
                : LocalDate.now();

        // Swap if from > to
        if (toDate.isBefore(fromDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        // Query all exercise logs in the date range
        List<ExerciseLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<ExerciseLog>()
                        .eq(ExerciseLog::getUserId, userId)
                        .ge(ExerciseLog::getExerciseDate, fromDate)
                        .le(ExerciseLog::getExerciseDate, toDate)
                        .isNull(ExerciseLog::getDeletedAt)
                        .orderByAsc(ExerciseLog::getExerciseDate)
                        .orderByAsc(ExerciseLog::getCreatedAt)
        );

        // Delegate to domain model for response building (includes validation)
        return ExerciseLog.toRangeResponse(logs, fromDate, toDate);
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

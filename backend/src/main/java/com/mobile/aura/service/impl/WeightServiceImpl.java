package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.domain.user.UserWeightLog;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.weight.UpdateInitialWeightReq;
import com.mobile.aura.dto.weight.WeightLatestResp;
import com.mobile.aura.dto.weight.WeightPointResp;
import com.mobile.aura.dto.weight.WeightRangeResp;
import com.mobile.aura.dto.weight.WeightSubmitReq;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.mapper.UserWeightLogMapper;
import com.mobile.aura.service.WeightService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of WeightService.
 * Service layer orchestrates workflows and delegates business logic to domain models.
 */
@Service
@RequiredArgsConstructor
public class WeightServiceImpl implements WeightService {

    private final UserWeightLogMapper logMapper;
    private final UserHealthProfileMapper healthProfileMapper;

    @Override
    @Transactional
    public void submit(Long userId, WeightSubmitReq req) {
        LocalDate day = SingleDateReq.parseDateOrToday(req.getDate());

        UserWeightLog existing = logMapper.selectOne(new LambdaQueryWrapper<UserWeightLog>()
                .eq(UserWeightLog::getUserId, userId)
                .eq(UserWeightLog::getRecordedAt, day));

        if (existing == null) {
            UserWeightLog newLog = UserWeightLog.create(userId, req);
            logMapper.insert(newLog);
        } else {
            existing.update(req);
            logMapper.updateById(existing);
        }

        updateHealthProfile(userId, req.getWeightKg(), day);
    }

    @Override
    @Transactional(readOnly = true)
    public WeightRangeResp range(Long userId, String start, String end) {
        LocalDate startDate = SingleDateReq.parseDateOrToday(start);
        LocalDate endDate = SingleDateReq.parseDateOrToday(end);

        List<WeightPointResp> items = logMapper.selectList(new LambdaQueryWrapper<UserWeightLog>()
                        .eq(UserWeightLog::getUserId, userId)
                        .ge(UserWeightLog::getRecordedAt, startDate)
                        .le(UserWeightLog::getRecordedAt, endDate)
                        .orderByAsc(UserWeightLog::getRecordedAt))
                .stream()
                .map(UserWeightLog::toResponse)
                .toList();

        return new WeightRangeResp(items);
    }

    @Override
    @Transactional(readOnly = true)
    public WeightLatestResp latest(Long userId) {
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            return new WeightLatestResp(null, null, null, null, null, null);
        }

        return profile.toWeightLatestResp();
    }

    /**
     * Update health profile with weight information.
     * Optimized: directly uses submitted data without extra database queries.
     *
     * @param userId user ID
     * @param weightKg submitted weight in kilograms
     * @param recordDate date of weight record
     */
    private void updateHealthProfile(Long userId, Double weightKg, LocalDate recordDate) {
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            // First time submission - use submitted data as both initial and latest
            UserHealthProfile newProfile = UserHealthProfile.createInitial(userId, weightKg, recordDate);
            healthProfileMapper.insert(newProfile);
            return;
        }

        // Profile exists - check if we need to update initial or latest
        boolean needUpdate = false;

        if (profile.shouldUpdateInitial(recordDate)) {
            // New record is earlier than current initial - use submitted data directly
            profile.updateInitialWeight(weightKg, recordDate);
            needUpdate = true;
        }

        if (profile.shouldUpdateLatest(recordDate)) {
            // New record is latest or equal to latest - use submitted data directly
            profile.updateLatestWeight(weightKg, recordDate);
            needUpdate = true;
        }

        if (needUpdate) {
            healthProfileMapper.updateById(profile);
        }
    }

    @Override
    @Transactional
    public void updateInitialWeight(Long userId, UpdateInitialWeightReq req) {
        LocalDate recordDate = SingleDateReq.parseDateOrToday(req.getDate());

        // Get health profile
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            throw new BizException(CommonStatusEnum.HEALTH_PROFILE_NOT_FOUND);
        }

        // Update initial weight
        profile.updateInitialWeight(req.getWeightKg(), recordDate);
        healthProfileMapper.updateById(profile);
    }
}

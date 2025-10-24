package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.health.WaterIntake;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.dto.water.*;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.mapper.WaterIntakeMapper;
import com.mobile.aura.service.WaterService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Implementation of WaterService.
 * Service layer orchestrates workflows and delegates business logic to domain models.
 */
@Service
@RequiredArgsConstructor
public class WaterServiceImpl implements WaterService {

    private final WaterIntakeMapper mapper;
    private final UserHealthProfileMapper healthProfileMapper;

    @Override
    @Transactional
    public void submit(Long userId, LocalDate date, int amountMl) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        WaterIntake existing = mapper.selectOne(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .eq(WaterIntake::getIntakeDate, effectiveDate));

        if (existing == null) {
            WaterIntake newRecord = WaterIntake.create(userId, effectiveDate, amountMl);
            mapper.insert(newRecord);
        } else {
            existing.updateAmount(amountMl);
            mapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void increment(Long userId, LocalDate date, int incrementMl) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        WaterIntake existing = mapper.selectOne(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .eq(WaterIntake::getIntakeDate, effectiveDate));

        if (existing == null) {
            WaterIntake newRecord = WaterIntake.create(userId, effectiveDate, incrementMl);
            mapper.insert(newRecord);
        } else {
            existing.incrementAmount(incrementMl);
            mapper.updateById(existing);
        }

        // Add to quick records
        addToQuickRecords(userId, incrementMl);
    }

    /**
     * Add amount to user's quick records list.
     */
    private void addToQuickRecords(Long userId, int amountMl) {
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            profile = UserHealthProfile.builder()
                    .userId(userId)
                    .build();
            healthProfileMapper.insert(profile);
        }

        profile.addToQuickRecords(amountMl);
        healthProfileMapper.updateById(profile);
    }

    @Override
    @Transactional
    public void deleteDay(Long userId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        mapper.delete(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .eq(WaterIntake::getIntakeDate, effectiveDate));
    }

    @Override
    @Transactional(readOnly = true)
    public WaterDayResp day(Long userId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        WaterIntake record = mapper.selectOne(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .eq(WaterIntake::getIntakeDate, effectiveDate));

        if (record != null) {
            return record.toResponse();
        } else {
            return new WaterDayResp(
                    effectiveDate.format(ofPattern("yyyy-MM-dd")),
                    0
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WaterRangeResp range(Long userId, String from, String to) {
        LocalDate[] dateRange = WaterIntake.normalizeDateRange(from, to);
        LocalDate fromDate = dateRange[0];
        LocalDate toDate = dateRange[1];

        List<WaterIntake> records = mapper.selectList(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .ge(WaterIntake::getIntakeDate, fromDate)
                .le(WaterIntake::getIntakeDate, toDate));

        Map<LocalDate, WaterIntake> dataMap = new HashMap<>();
        for (WaterIntake record : records) {
            dataMap.put(record.getIntakeDate(), record);
        }

        List<WaterDayResp> items = WaterIntake.toRangeResponse(dataMap, fromDate, toDate);
        return new WaterRangeResp(items);
    }

    @Override
    @Transactional(readOnly = true)
    public WaterConfigResp getConfig(Long userId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            throw new BizException(CommonStatusEnum.WATER_GOAL_NOT_SET);
        }

        WaterIntake todayIntake = mapper.selectOne(new LambdaQueryWrapper<WaterIntake>()
                .eq(WaterIntake::getUserId, userId)
                .eq(WaterIntake::getIntakeDate, effectiveDate));

        int currentMl = todayIntake != null ? todayIntake.getAmountMl() : 0;

        return profile.toWaterConfigResp(currentMl);
    }

    @Override
    @Transactional
    public void updateConfig(Long userId, WaterConfigUpdateReq req) {
        UserHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<UserHealthProfile>()
                        .eq(UserHealthProfile::getUserId, userId)
        );

        if (profile == null) {
            profile = UserHealthProfile.builder()
                    .userId(userId)
                    .build();
            healthProfileMapper.insert(profile);
        }

        if (req.getQuickRecordsMl() != null) {
            profile.updateQuickRecords(req.getQuickRecordsMl());
        }

        if (req.getGoalWaterIntakeMl() != null) {
            profile.updateGoalWaterIntake(req.getGoalWaterIntakeMl());
        }

        healthProfileMapper.updateById(profile);
    }
}

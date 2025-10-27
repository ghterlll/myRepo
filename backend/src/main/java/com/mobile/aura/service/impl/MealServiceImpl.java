package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.health.MealLog;
import com.mobile.aura.domain.aggregate.UserFoodItem;
import com.mobile.aura.dto.meal.DailySummaryResp;
import com.mobile.aura.dto.meal.MealAddFreeInputReq;
import com.mobile.aura.dto.meal.MealAddFromSourceReq;
import com.mobile.aura.dto.meal.MealEditReq;
import com.mobile.aura.mapper.MealLogMapper;
import com.mobile.aura.mapper.UserFoodItemMapper;
import com.mobile.aura.service.MealService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealLogMapper mealLogMapper;
    private final UserFoodItemMapper userFoodItemMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DailySummaryResp daySummary(Long userId, LocalDate date) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;

        // Service layer: Only responsible for data access via mapper
        List<MealLog> mealLogs = mealLogMapper.selectList(new LambdaQueryWrapper<MealLog>()
                .eq(MealLog::getUserId, userId)
                .eq(MealLog::getMealDate, effectiveDate)
                .isNull(MealLog::getDeletedAt)
                .orderByAsc(MealLog::getMealDate)
                .orderByAsc(MealLog::getId));

        // Domain layer: Business logic for building summary
        return MealLog.buildDailySummary(mealLogs, DATE_FORMATTER.format(effectiveDate));
    }

    @Override
    @Transactional
    public Long addFromSource(Long userId, MealAddFromSourceReq request) {
        // Fetch food item from database
        UserFoodItem userFoodItem = userFoodItemMapper.selectById(request.getSourceId());

        if (userFoodItem == null) {
            throw new BizException(CommonStatusEnum.FOOD_NOT_FOUND);
        }

        MealLog mealLog = MealLog.createFromSource(userId, request, userFoodItem);
        mealLogMapper.insert(mealLog);
        return mealLog.getId();
    }

    @Override
    @Transactional
    public Long addFreeInput(Long userId, MealAddFreeInputReq request) {
        MealLog mealLog = MealLog.createFromFreeInput(userId, request);
        mealLogMapper.insert(mealLog);
        return mealLog.getId();
    }

    @Override
    @Transactional
    public void edit(Long userId, Long mealId, MealEditReq request) {
        MealLog mealLog = mealLogMapper.selectById(mealId);

        if (mealLog == null) {
            throw new BizException(CommonStatusEnum.MEAL_LOG_NOT_FOUND);
        }

        mealLog.ensureAccessibleBy(userId);

        // Fetch food item if this meal has a source (for calorie recalculation)
        UserFoodItem userFoodItem = null;
        if (mealLog.getSourceId() != null) {
            userFoodItem = userFoodItemMapper.selectById(mealLog.getSourceId());
        }

        mealLog.updateFromRequest(request, userFoodItem);
        mealLogMapper.updateById(mealLog);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long mealId) {
        MealLog mealLog = mealLogMapper.selectById(mealId);

        if (mealLog == null) {
            throw new BizException(CommonStatusEnum.MEAL_LOG_NOT_FOUND);
        }

        mealLog.ensureAccessibleBy(userId);
        mealLog.markAsDeleted();
        mealLogMapper.updateById(mealLog);
    }
}

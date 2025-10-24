package com.mobile.aura.service.impl;

import com.mobile.aura.domain.aggregate.UserFoodItem;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.food.*;
import com.mobile.aura.mapper.UserFoodItemMapper;
import com.mobile.aura.service.FoodService;
import com.mobile.aura.support.BizException;
import com.mobile.aura.constant.CommonStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of FoodService.
 * Service layer orchestrates workflows and delegates business logic to domain models.
 * Follows DDD principles: thin service layer, rich domain model.
 */
@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final UserFoodItemMapper userMapper;

    /**
     * Search user's custom food items with cursor pagination.
     * Note: System food library has been moved to frontend.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodItemResp> search(Long userId, FoodSearchReq req) {
        String keyword = (req.getQ() == null || req.getQ().isBlank()) ? null : req.getQ().trim();
        int limit = req.getPageLimit().getValue();
        Long cursor = req.getCursorAsLong();

        List<UserFoodItem> results = userMapper.searchWithCursor(userId, keyword, cursor, limit + 1);

        return UserFoodItem.toSearchResponse(results, limit);
    }

    /**
     * Get food item detail.
     * Workflow: Fetch entity -> Validate ownership -> Return response
     */
    @Override
    @Transactional(readOnly = true)
    public FoodItemResp detail(Long userId, Long id) {
        UserFoodItem item = userMapper.selectById(id);
        if (item == null) {
            throw new BizException(CommonStatusEnum.NOT_FOUND);
        }

        item.ensureOwnedBy(userId);
        return item.toResponse();
    }

    /**
     * Create a new custom food item for the user.
     * Workflow: Create entity -> Persist -> Return ID
     */
    @Override
    @Transactional
    public Long createMy(Long userId, FoodCreateReq req) {
        UserFoodItem item = UserFoodItem.create(userId, req);

        try {
            userMapper.insert(item);
        } catch (DuplicateKeyException e) {
            throw new BizException(CommonStatusEnum.FOOD_NAME_DUPLICATE);
        }

        return item.getId();
    }

    /**
     * Update an existing custom food item.
     * Workflow: Fetch entity -> Validate ownership -> Update -> Persist
     */
    @Override
    @Transactional
    public void updateMy(Long userId, Long id, FoodUpdateReq req) {
        UserFoodItem item = userMapper.selectById(id);
        if (item == null) {
            throw new BizException(CommonStatusEnum.NOT_FOUND);
        }

        item.ensureOwnedBy(userId);
        item.update(req);

        try {
            userMapper.updateById(item);
        } catch (DuplicateKeyException e) {
            throw new BizException(CommonStatusEnum.FOOD_NAME_DUPLICATE);
        }
    }

    /**
     * Delete a custom food item.
     * Workflow: Delete by user ID and item ID (ownership enforced by DB query)
     */
    @Override
    @Transactional
    public void deleteMy(Long userId, Long id) {
        int deletedCount = userMapper.deleteByUserIdAndId(userId, id);
        UserFoodItem.ensureDeleted(deletedCount);
    }

    /**
     * List user's custom food items with cursor-based pagination.
     * Uses ID-based cursor for consistent and efficient pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodItemResp> myList(Long userId, FoodListReq req) {
        int limit = req.getPageLimit().getValue();
        Long cursor = req.getCursorAsLong();

        List<UserFoodItem> results = userMapper.listWithCursor(userId, cursor, limit + 1);

        return UserFoodItem.toListResponse(results, limit);
    }
}

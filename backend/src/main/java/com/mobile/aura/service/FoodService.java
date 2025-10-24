package com.mobile.aura.service;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.food.*;


/**
 * Food management service interface.
 * Handles user custom food items with cursor-based pagination.
 * System food library has been moved to frontend for better performance.
 */
public interface FoodService {

    /**
     * Search food items with cursor-based pagination.
     * Supports keyword search and scope filtering.
     *
     * @param userId the ID of the user performing the search
     * @param req    search request containing query, scope, cursor, and limit
     * @return paginated response with food items
     */
    PageResponse<FoodItemResp> search(Long userId, FoodSearchReq req);

    /**
     * Get detailed information about a specific food item.
     *
     * @param userId the ID of the user requesting the detail
     * @param id     the food item ID
     * @return detailed food item information
     * @throws com.mobile.aura.support.BizException if food not found or access denied
     */
    FoodItemResp detail(Long userId, Long id);

    /**
     * Create a new custom food item for the user.
     *
     * @param userId the ID of the user creating the food item
     * @param req    food creation request with name, unit, calories, and image
     * @return the ID of the newly created food item
     * @throws com.mobile.aura.support.BizException if food name already exists for this user
     */
    Long createMy(Long userId, FoodCreateReq req);

    /**
     * Update an existing custom food item.
     *
     * @param userId the ID of the user updating the food item
     * @param id     the food item ID to update
     * @param req    food update request with optional fields to update
     * @throws com.mobile.aura.support.BizException if food not found, access denied, or name conflict
     */
    void updateMy(Long userId, Long id, FoodUpdateReq req);

    /**
     * Delete a custom food item.
     *
     * @param userId the ID of the user deleting the food item
     * @param id     the food item ID to delete
     */
    void deleteMy(Long userId, Long id);

    /**
     * List user's custom food items with cursor-based pagination.
     *
     * @param userId the ID of the user
     * @param req    list request containing cursor and limit
     * @return paginated response with food items
     */
    PageResponse<FoodItemResp> myList(Long userId, FoodListReq req);

}

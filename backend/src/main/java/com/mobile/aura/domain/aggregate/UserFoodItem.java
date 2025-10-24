package com.mobile.aura.domain.aggregate;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.food.FoodCreateReq;
import com.mobile.aura.dto.food.FoodItemResp;
import com.mobile.aura.dto.food.FoodUpdateReq;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * User custom food item entity (Aggregate Root).
 * Represents food items created by users with custom nutritional information.
 * Encapsulates all business logic related to user food items.
 */
@Data
@TableName("user_food_item")
public class UserFoodItem {
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TableId
    private Long id;
    private Long userId;
    private String name;
    private String unitName;
    private Integer kcalPerUnit;
    private String imageUrl;
    private Integer enabled;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new user food item.
     *
     * @param userId the owner's user ID
     * @param req    creation request with food details
     * @return newly created UserFoodItem (not yet persisted)
     */
    public static UserFoodItem create(Long userId, FoodCreateReq req) {
        UserFoodItem item = new UserFoodItem();
        item.userId = userId;
        item.name = req.getName();
        item.unitName = req.getUnitName();
        item.kcalPerUnit = req.getKcalPerUnit();
        item.imageUrl = req.getImageUrl();
        item.enabled = 1;
        return item;
    }

    /**
     * Update this food item with values from update request.
     * Only updates non-null fields from the request.
     *
     * @param req update request with optional fields
     */
    public void update(FoodUpdateReq req) {
        if (req.getName() != null) {
            this.name = req.getName();
        }
        if (req.getUnitName() != null) {
            this.unitName = req.getUnitName();
        }
        if (req.getKcalPerUnit() != null) {
            this.kcalPerUnit = req.getKcalPerUnit();
        }
        if (req.getImageUrl() != null) {
            this.imageUrl = req.getImageUrl();
        }
    }

    /**
     * Validate that this food item belongs to the given user.
     * Throws exception if validation fails.
     *
     * @param userId the user ID to check ownership against
     * @throws BizException if item doesn't belong to user
     */
    public void ensureOwnedBy(Long userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw new BizException(CommonStatusEnum.NOT_FOUND);
        }
    }

    /**
     * Ensure food item was successfully deleted.
     *
     * @param deletedCount the number of rows deleted
     * @throws BizException if no rows were deleted (food item not found or not owned by user)
     */
    public static void ensureDeleted(int deletedCount) {
        Optional.of(deletedCount)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.FOOD_NOT_FOUND);
                });
    }

    /**
     * Convert this entity to a response DTO for API output.
     * User food items don't have alias or category fields (those are for system foods).
     *
     * @return FoodItemResp DTO with formatted data
     */
    public FoodItemResp toResponse() {
        return new FoodItemResp(
                this.id,
                this.name,
                null,  // User items don't have alias
                null,  // User items don't have category
                this.unitName,
                this.kcalPerUnit,
                this.imageUrl,
                this.createdAt == null ? null : DATETIME_FORMATTER.format(this.createdAt)
        );
    }

    /**
     * Build a paginated list response from query results.
     * Handles cursor-based pagination logic: extracts actual items, converts to DTOs,
     * and determines next cursor and hasMore flag.
     *
     * @param results query results (limit + 1 items)
     * @param limit   the page size limit
     * @return PageResponse with items, cursor, and pagination metadata
     */
    public static PageResponse<FoodItemResp> toListResponse(List<UserFoodItem> results, int limit) {
        List<FoodItemResp> items = results.stream()
                .map(UserFoodItem::toResponse)
                .toList();

        return PageResponse.paginate(items, limit, item -> item.getId().toString());
    }

    /**
     * Build a paginated search response from query results.
     * Similar to toListResponse but returns PageResponse type for search operations.
     *
     * @param results query results (limit + 1 items)
     * @param limit   the page size limit
     * @return PageResponse with items, cursor, and pagination metadata
     */
    public static PageResponse<FoodItemResp> toSearchResponse(List<UserFoodItem> results, int limit) {
        return toListResponse(results, limit);
    }
}
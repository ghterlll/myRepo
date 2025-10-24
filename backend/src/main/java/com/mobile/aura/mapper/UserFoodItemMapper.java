package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.aggregate.UserFoodItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for user custom food items.
 * Complex queries are defined in UserFoodItemMapper.xml for better maintainability.
 */
@Mapper
public interface UserFoodItemMapper extends BaseMapper<UserFoodItem> {

    /**
     * Search user's food items with optional keyword and cursor pagination.
     *
     * @param userId the user ID
     * @param keyword search keyword (nullable)
     * @param cursor cursor for pagination (nullable, filters id < cursor)
     * @param limit max number of items to return
     * @return list of food items
     */
    List<UserFoodItem> searchWithCursor(@Param("userId") Long userId,
                                         @Param("keyword") String keyword,
                                         @Param("cursor") Long cursor,
                                         @Param("limit") int limit);

    /**
     * List user's food items with cursor pagination.
     *
     * @param userId the user ID
     * @param cursor cursor for pagination (nullable, filters id < cursor)
     * @param limit max number of items to return
     * @return list of food items
     */
    List<UserFoodItem> listWithCursor(@Param("userId") Long userId,
                                       @Param("cursor") Long cursor,
                                       @Param("limit") int limit);

    /**
     * Delete a user's food item by ID.
     *
     * @param userId the user ID
     * @param id the food item ID
     * @return number of deleted rows
     */
    int deleteByUserIdAndId(@Param("userId") Long userId,
                            @Param("id") Long id);
}
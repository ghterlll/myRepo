package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserSocialStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for UserSocialStats domain model.
 * All SQL statements are defined in UserSocialStatsMapper.xml
 */
@Mapper
public interface UserSocialStatsMapper extends BaseMapper<UserSocialStats> {

    /**
     * Increment or decrement follow count.
     *
     * @param userId user ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incFollowCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * Increment or decrement fans count.
     *
     * @param userId user ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incFansCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * Increment or decrement post count.
     *
     * @param userId user ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incPostCount(@Param("userId") Long userId, @Param("delta") int delta);
}

package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserHealthProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for UserHealthProfile domain model.
 */
@Mapper
public interface UserHealthProfileMapper extends BaseMapper<UserHealthProfile> {

    /**
     * Find user health profile by user ID.
     *
     * @param userId the user ID
     * @return user health profile, or null if not found
     */
    UserHealthProfile findByUserId(@Param("userId") Long userId);
}

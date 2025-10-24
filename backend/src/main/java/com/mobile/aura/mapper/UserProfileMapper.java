package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
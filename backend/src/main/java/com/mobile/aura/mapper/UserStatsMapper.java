package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserStats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserStatsMapper   extends BaseMapper<UserStats> {}
package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.event.EventLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for EventLog table.
 * Handles persistence of user behavior tracking events.
 */
@Mapper
public interface EventLogMapper extends BaseMapper<EventLog> {
}

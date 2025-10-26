package com.mobile.aura.service.impl;

import com.mobile.aura.domain.event.EventLog;
import com.mobile.aura.mapper.EventLogMapper;
import com.mobile.aura.service.EventLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Event logging service implementation.
 * Records user behavior for recommendation system analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogServiceImpl implements EventLogService {

    private final EventLogMapper eventLogMapper;

    @Override
    public void logEvent(Long userId, Long itemId, String eventType) {
        try {
            EventLog event = EventLog.create(userId, itemId, eventType);
            eventLogMapper.insert(event);
            log.debug("Logged event: user={}, item={}, type={}", userId, itemId, eventType);
        } catch (Exception e) {
            // Don't let logging failures affect business logic
            log.error("Failed to log event: user={}, item={}, type={}", userId, itemId, eventType, e);
        }
    }

    @Override
    public void logEventWithContext(Long userId, Long itemId, String eventType,
                                     String deviceType, Double geoLat, Double geoLon, String city) {
        try {
            EventLog event = EventLog.create(userId, itemId, eventType)
                    .withContext(deviceType, geoLat, geoLon, city);
            eventLogMapper.insert(event);
            log.debug("Logged event with context: user={}, item={}, type={}, device={}, city={}",
                    userId, itemId, eventType, deviceType, city);
        } catch (Exception e) {
            log.error("Failed to log event with context: user={}, item={}, type={}",
                    userId, itemId, eventType, e);
        }
    }

    @Async
    @Override
    public void logEventAsync(Long userId, Long itemId, String eventType) {
        logEvent(userId, itemId, eventType);
    }
}

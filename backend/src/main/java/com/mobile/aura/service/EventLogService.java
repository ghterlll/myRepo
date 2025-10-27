package com.mobile.aura.service;

/**
 * Service interface for event logging.
 * Tracks user behavior for recommendation system.
 */
public interface EventLogService {

    /**
     * Log a simple event (expose, click, etc.)
     *
     * @param userId user ID
     * @param itemId content/post ID
     * @param eventType event type (expose/click/like/favorite/share/comment)
     */
    void logEvent(Long userId, Long itemId, String eventType);

    /**
     * Log an event with additional context
     *
     * @param userId user ID
     * @param itemId content/post ID
     * @param eventType event type
     * @param deviceType device type (iOS/Android/Web)
     * @param geoLat user's current latitude
     * @param geoLon user's current longitude
     * @param city user's current city
     */
    void logEventWithContext(Long userId, Long itemId, String eventType,
                             String deviceType, Double geoLat, Double geoLon, String city);

    /**
     * Log an event asynchronously (non-blocking)
     * Suitable for high-frequency events like expose
     *
     * @param userId user ID
     * @param itemId content/post ID
     * @param eventType event type
     */
    void logEventAsync(Long userId, Long itemId, String eventType);
}

package com.mobile.aura.domain.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event log domain model for user behavior tracking.
 * Records user interactions with content for recommendation system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("event_log")
public class EventLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long itemId;           // Post/Content ID
    private String eventType;      // expose/click/like/favorite/share/comment
    private LocalDateTime ts;      // Event timestamp

    private Long sessionId;        // Session identifier (optional)
    private BigDecimal dwellTime;  // Time spent on content in seconds (optional)
    private String deviceType;     // iOS/Android/Web
    private String networkType;    // WiFi/4G/5G
    private Double geoLat;         // User's current latitude
    private Double geoLon;         // User's current longitude
    private String city;           // User's current city

    private LocalDateTime createdAt;

    /**
     * Event types
     */
    public static class EventType {
        public static final String EXPOSE = "expose";     // Content shown to user
        public static final String CLICK = "click";       // User clicked/viewed content
        public static final String LIKE = "like";         // User liked content
        public static final String FAVORITE = "favorite"; // User bookmarked/favorited content
        public static final String SHARE = "share";       // User shared content
        public static final String COMMENT = "comment";   // User commented on content
    }

    /**
     * Factory method to create an event log entry.
     *
     * @param userId user ID
     * @param itemId content/post ID
     * @param eventType event type (use EventType constants)
     * @return EventLog instance
     */
    public static EventLog create(Long userId, Long itemId, String eventType) {
        return EventLog.builder()
                .userId(userId)
                .itemId(itemId)
                .eventType(eventType)
                .ts(LocalDateTime.now())
                .deviceType("Android")  // Default, should be overridden
                .networkType("WiFi")    // Default
                .geoLat(-37.81361100)   // Default: Melbourne
                .geoLon(144.96305600)
                .city("Melbourne")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Set user context (device, location, etc.)
     *
     * @param deviceType device type
     * @param geoLat latitude
     * @param geoLon longitude
     * @param city city name
     * @return this instance for chaining
     */
    public EventLog withContext(String deviceType, Double geoLat, Double geoLon, String city) {
        if (deviceType != null) this.deviceType = deviceType;
        if (geoLat != null) this.geoLat = geoLat;
        if (geoLon != null) this.geoLon = geoLon;
        if (city != null) this.city = city;
        return this;
    }

    /**
     * Set session information
     *
     * @param sessionId session ID
     * @return this instance for chaining
     */
    public EventLog withSession(Long sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Set dwell time (time spent on content)
     *
     * @param seconds time in seconds
     * @return this instance for chaining
     */
    public EventLog withDwellTime(double seconds) {
        this.dwellTime = BigDecimal.valueOf(seconds);
        return this;
    }
}

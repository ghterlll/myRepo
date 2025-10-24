package com.mobile.aura.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExposureDto {
    private Long id;
    private String contentId;

    // The 5 key fields requested
    private LocalDateTime exposureTime;  // 1. 时间 (Time)
    private Integer weekday;              // 2. 周几 (Day of week, 1-7)
    private String weather;               // 3. 天气 (Weather)
    private String device;                // 4. 设备 (Device)
    private String city;                  // 5. 城市 (City)

    // Additional context fields
    private String platform;
    private Long authorId;
    private String tags;
}

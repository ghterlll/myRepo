package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// domain/ContentExposure.java
@Data
@TableName("content_exposure")
public class ContentExposure {
    @TableId
    private Long id;
    private Long userId;
    private Long userFeatureId;     // Reference to user_features table
    private Long contentFeatureId;  // Reference to content_feature table
    private String contentId;
    private LocalDateTime exposureTime;
    private String platform;
    private Integer weekday;
    private String weather;
    private String device;
    private String city;
}

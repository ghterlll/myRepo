package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_features")
public class UserFeature {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 5 user features
    private Integer age;              // 1. Age
    private String region;            // 2. Permanent residence
    private Integer activityLvl;      // 3. Activity level
    private String interests;         // 4. Interest tags
    private Integer followCount;      // 5. Following count

    private LocalDateTime snapshotTime;  // Feature snapshot timestamp
    private LocalDateTime createdAt;
}

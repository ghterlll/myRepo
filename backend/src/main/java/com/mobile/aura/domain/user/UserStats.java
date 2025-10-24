package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

// domain/UserStats.java
@Data
@TableName("user_stats")
public class UserStats {
    @TableId
    private Long userId;
    private Integer followCount;
    private Integer followerCount;
    private Integer postCount;
    private Date updatedAt;
}

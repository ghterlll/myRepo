package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Content feature snapshot for analytics
 * Stores post metadata at the time of exposure
 */
@Data
@TableName("content_feature")
public class ContentFeature {
    @TableId
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("author_id")
    private Long authorId;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("heat_score")
    private BigDecimal heatScore;

    @TableField("tag_ids")
    private String tagIds;  // Comma-separated tag IDs

    @TableField("snapshot_time")
    private LocalDateTime snapshotTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Factory method to create snapshot from Post and PostStatistics
     */
    public static ContentFeature createFromPost(Post post, String tagIds, BigDecimal heatScore) {
        ContentFeature feature = new ContentFeature();
        feature.setPostId(post.getId());
        feature.setAuthorId(post.getAuthorId());
        feature.setPublishTime(post.getCreatedAt());
        feature.setHeatScore(heatScore);
        feature.setTagIds(tagIds);
        feature.setSnapshotTime(LocalDateTime.now());
        return feature;
    }
}

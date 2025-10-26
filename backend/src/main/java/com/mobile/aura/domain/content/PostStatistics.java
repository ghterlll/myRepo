package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Post statistics domain model.
 * Separated from Post entity for better performance and scalability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_statistics")
public class PostStatistics {

    @TableId(type = IdType.AUTO)
    private Long postId;
    private Integer likeCount;
    private Integer commentCount;
    private Integer bookmarkCount;
    private BigDecimal heatScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create initial statistics for a new post.
     *
     * @param postId the ID of the post
     * @return new PostStatistics instance with zero counts
     */
    public static PostStatistics createForPost(Long postId) {
        return PostStatistics.builder()
                .postId(postId)
                .likeCount(0)
                .commentCount(0)
                .bookmarkCount(0)
                .heatScore(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Ensure statistics update succeeded.
     *
     * @param updatedCount the number of rows updated
     * @throws BizException if update failed (post statistics not found)
     */
    public static void ensureUpdated(int updatedCount) {
        Optional.of(updatedCount)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.POST_NOT_FOUND);
                });
    }
}

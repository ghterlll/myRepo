package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for PostStatistics operations.
 * All SQL statements are defined in PostStatisticsMapper.xml
 */
@Mapper
public interface PostStatisticsMapper extends BaseMapper<PostStatistics> {

    /**
     * Increment or decrement like count.
     *
     * @param postId the post ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * Increment or decrement comment count.
     *
     * @param postId the post ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * Increment or decrement bookmark count.
     *
     * @param postId the post ID
     * @param delta increment value (can be negative)
     * @return number of rows updated
     */
    int incBookmarkCount(@Param("postId") Long postId, @Param("delta") int delta);
}

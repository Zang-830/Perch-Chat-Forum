package com.campus.community.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.community.post.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PostMapper extends BaseMapper<Post> {
    @Select("""
            <script>
            SELECT * FROM posts
            WHERE status = 'PUBLISHED'
            <if test='categoryId != null'>AND category_id = #{categoryId}</if>
            <if test='keyword != null and keyword != ""'>
              AND (LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                   OR LOWER(summary) LIKE CONCAT('%', LOWER(#{keyword}), '%'))
            </if>
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Post> selectFeed(@Param("keyword") String keyword,
                          @Param("categoryId") Long categoryId,
                          @Param("offset") int offset,
                          @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM posts
            WHERE status = 'PUBLISHED'
            <if test='categoryId != null'>AND category_id = #{categoryId}</if>
            <if test='keyword != null and keyword != ""'>
              AND (LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                   OR LOWER(summary) LIKE CONCAT('%', LOWER(#{keyword}), '%'))
            </if>
            </script>
            """)
    long countFeed(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    @Update("UPDATE posts SET like_count = GREATEST(like_count + #{delta}, 0), updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
    int changeLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Update("UPDATE posts SET comment_count = comment_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{postId}")
    int incrementCommentCount(@Param("postId") Long postId);
}

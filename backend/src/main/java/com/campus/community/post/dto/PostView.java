package com.campus.community.post.dto;

import com.campus.community.category.dto.CategoryView;
import com.campus.community.user.dto.UserView;

import java.time.LocalDateTime;
import java.util.List;

public record PostView(
        Long id,
        String title,
        String summary,
        String content,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt,
        UserView author,
        CategoryView category,
        List<String> imageUrls,
        boolean likedByMe
) {
}

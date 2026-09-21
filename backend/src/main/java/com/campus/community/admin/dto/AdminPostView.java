package com.campus.community.admin.dto;

import java.time.LocalDateTime;

public record AdminPostView(
        Long id,
        String title,
        String authorName,
        String categoryName,
        String status,
        int likeCount,
        int commentCount,
        long imageCount,
        LocalDateTime createdAt
) {
}


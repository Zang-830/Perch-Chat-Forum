package com.campus.community.admin.dto;

public record DashboardStats(
        long totalUsers,
        long activeUsers,
        long bannedUsers,
        long totalPosts,
        long publishedPosts,
        long hiddenPosts,
        long totalComments,
        long totalImages,
        long pendingReports
) {
}


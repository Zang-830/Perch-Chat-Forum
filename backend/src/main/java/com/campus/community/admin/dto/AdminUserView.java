package com.campus.community.admin.dto;

import java.time.LocalDateTime;

public record AdminUserView(
        Long id,
        String username,
        String nickname,
        String role,
        String status,
        LocalDateTime createdAt
) {
}


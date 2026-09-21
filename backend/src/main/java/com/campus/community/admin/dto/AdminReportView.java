package com.campus.community.admin.dto;

import java.time.LocalDateTime;

public record AdminReportView(
        Long id,
        String targetType,
        Long targetId,
        String targetTitle,
        String reporterName,
        String reason,
        String status,
        String handleNote,
        LocalDateTime createdAt,
        LocalDateTime handledAt
) {
}


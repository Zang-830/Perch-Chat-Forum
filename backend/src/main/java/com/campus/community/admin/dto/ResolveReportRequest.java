package com.campus.community.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotBlank(message = "处理结果不能为空") String status,
        @Size(max = 500, message = "处理备注不能超过 500 个字符") String note
) {
}


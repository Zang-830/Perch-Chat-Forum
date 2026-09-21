package com.campus.community.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank(message = "请选择举报类型") String targetType,
        @NotNull(message = "举报对象不能为空") Long targetId,
        @NotBlank(message = "请填写举报原因")
        @Size(max = 500, message = "举报原因不能超过 500 个字符") String reason
) {
}

